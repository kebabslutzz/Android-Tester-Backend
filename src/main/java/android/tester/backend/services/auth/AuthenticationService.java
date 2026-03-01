package android.tester.backend.services.auth;

import android.tester.backend.dtos.user.*;
import android.tester.backend.entities.RefreshToken;
import android.tester.backend.entities.User;
import android.tester.backend.enums.Role;
import android.tester.backend.exceptions.ConflictException;
import android.tester.backend.exceptions.NotFoundException;
import android.tester.backend.repositories.RefreshTokenRepository;
import android.tester.backend.repositories.UserRepository;
import android.tester.backend.services.logging.LogService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final RefreshTokenRepository refreshTokenRepository;
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
    var refreshToken = jwtService.generateRefreshToken(user);

    // Records are immutable and instantiated via constructor
    return new AuthenticationResponseRecord(
      jwtToken,
      refreshToken,
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

    var user = repository.findByEmail(request.email());
    if (user == null) {
      throw new BadCredentialsException("User not found");
    }

    var jwtToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);

    revokeAllUserTokens(user);
    saveUserRefreshToken(user, refreshToken);

    return new AuthenticationResponseRecord(
      jwtToken,
      refreshToken,
      user.getUsername(),
      user.getId().toString()
    );
  }

  @Transactional
  public AuthenticationResponseRecord refreshToken(RefreshTokenRequestRecord request) {
    String refreshToken = request.refreshToken();
    String userEmail = jwtService.extractUsername(refreshToken);

    if (userEmail != null) {
      User user = this.repository.findByEmail(userEmail);
      if (user != null && jwtService.isTokenValid(refreshToken, user)) {
        // Check if token exists in DB and is not revoked/expired
        var tokenRecord = refreshTokenRepository.findByToken(refreshToken)
          .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));

        if (tokenRecord.isRevoked()) {
          throw new BadCredentialsException("Refresh token is revoked");
        }

        // Rotate Refresh Token (optional security measure, beneficial here)
        String accessToken = jwtService.generateToken(user);

        // Return existing refresh token or rotate based on policy.
        // Here we just return a new access token and keep the valid refresh token.
        return new AuthenticationResponseRecord(
          accessToken,
          refreshToken,
          user.getUsername(),
          user.getId().toString()
        );
      }
    }
    throw new BadCredentialsException("Invalid refresh token");
  }

  private void saveUserRefreshToken(User user, String jwtToken) {
    var token = RefreshToken.builder()
      .user(user)
      .token(jwtToken)
      .revoked(false)
      .expiryDate(Instant.now().plusMillis(604800000)) // 7 days matching JWT prop
      .build();
    refreshTokenRepository.save(token);
  }

  private void revokeAllUserTokens(User user) {
    var validUserTokens = refreshTokenRepository.findAllValidRefreshTokenByUser(user.getId());
    if (validUserTokens.isEmpty())
      return;
    validUserTokens.forEach(token -> {
      token.setRevoked(true);
    });
    refreshTokenRepository.saveAll(validUserTokens);
  }

  @Transactional
  public void changePassword(ChangePasswordRequestRecord request, Principal connectedUser) {
    var user = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

    // 1. Check if the current password is correct
    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new BadCredentialsException("Wrong password");
    }

    // 2. Check if the new password matches the confirmation
    if (!request.newPassword().equals(request.confirmationPassword())) {
      throw new BadCredentialsException("Passwords are not the same");
    }

    // 3. Update the password
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    repository.save(user);

    // Optional: Revoke all existing tokens so the user must log in again with new credentials?
    // revokeAllUserTokens(user);
  }

  public void updateUser(@Valid UpdateUserRequestRecord request, Principal connectedUser) {
    var principalUser = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

    // Fetch the attached/managed entity from DB to ensure we are editing live data
    var user = repository.findById(principalUser.getId())
      .orElseThrow(() -> new NotFoundException("User not found"));

    user.setUsername(request.name());
    user.setEmail(request.email());

    repository.save(user);
  }
}
