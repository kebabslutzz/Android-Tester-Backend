package android.tester.backend.services.auth;

import android.tester.backend.dtos.user.AuthenticationRequestRecord;
import android.tester.backend.dtos.user.AuthenticationResponseRecord;
import android.tester.backend.dtos.user.RegisterRequestRecord;
import android.tester.backend.entities.User;
import android.tester.backend.enums.Role;
import android.tester.backend.exceptions.ConflictException;
import android.tester.backend.repositories.UserRepository;
import android.tester.backend.services.logging.LogService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final LogService logger;

  @Transactional
  public AuthenticationResponseRecord register(RegisterRequestRecord request) {
    logger.addLog("Watch me register this loser: " + request.toString());
    // Simple check if user exists (should ideally throw exception)
    if (repository.existsByEmail(request.email())) {
      throw new ConflictException("Email already exists");
    }
    if (repository.existsByUsername(request.name())) {
      throw new ConflictException("Username already exists");
    }

    logger.addLog("User does not exist, creating...");

    var user = User.builder()
//      .id(UUID.randomUUID())
      .username(request.name())
      .email(request.email())
      .password(passwordEncoder.encode(request.password()))
      .role(Role.USER) // Default role
//      .version(0)
      .build();

    logger.addLog("User to be created: " + user.toString());

    user = repository.save(user);

    logger.addLog("User created: " + user);

    var jwtToken = jwtService.generateToken(user);

    // Records are immutable and instantiated via constructor
    return new AuthenticationResponseRecord(
      jwtToken,
      user.getUsername(),
      user.getId().toString()
    );
  }

  @Transactional
  public AuthenticationResponseRecord authenticate(AuthenticationRequestRecord request) {
    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        request.email(),
        request.password()
      )
    );

    // User is authenticated, retrieve user details by Email
    var user = repository.findByEmail(request.email());
    if (user == null) {
      // Should theoretically be caught by authenticationManager, but good for safety
      throw new BadCredentialsException("User not found");
    }

    var jwtToken = jwtService.generateToken(user);

    return new AuthenticationResponseRecord(
      jwtToken,
      user.getUsername(),
      user.getId().toString()
    );
  }
}
