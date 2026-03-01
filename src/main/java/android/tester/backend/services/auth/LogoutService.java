package android.tester.backend.services.auth;

import android.tester.backend.repositories.RefreshTokenRepository;
import android.tester.backend.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

  private final RefreshTokenRepository tokenRepository;
  private final JwtService jwtService;
  private final UserRepository userRepository; // Optional if you need to fetch user entity

  @Override
  public void logout(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication
  ) {
    final String authHeader = request.getHeader("Authorization");
    final String jwt;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return;
    }

    jwt = authHeader.substring(7);
    String userEmail = jwtService.extractUsername(jwt);

    if (userEmail != null) {
      var user = userRepository.findByEmail(userEmail);
      if (user != null) {
        // OPTION 1: Delete ALL refresh tokens for this user (Clear session completely)
        // This is the cleanest approach for preventing table bloat.
        var userTokens = tokenRepository.findAllValidRefreshTokenByUser(user.getId());
        if (!userTokens.isEmpty()) {
          tokenRepository.deleteAll(userTokens);
        }

        // Clear the security context
        SecurityContextHolder.clearContext();
      }
    }
  }
}
