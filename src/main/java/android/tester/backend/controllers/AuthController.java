package android.tester.backend.controllers;

import android.tester.backend.dtos.user.*;
import android.tester.backend.services.auth.AuthenticationService;
import android.tester.backend.services.logging.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthenticationService service;
  private final LogService logger;

  @ResponseStatus(value = HttpStatus.CREATED)
  @PostMapping("/register")
  public AuthenticationResponseRecord register(@Valid @RequestBody RegisterRequestRecord request) {
    logger.addLog("request received: " + request.toString());
    return service.register(request);
  }

  @PostMapping("/authenticate")
  public AuthenticationResponseRecord authenticate(@Valid @RequestBody AuthenticationRequestRecord request) {
    return service.authenticate(request);
  }

  @PostMapping("/refresh-token")
  public AuthenticationResponseRecord refreshToken(@Valid @RequestBody RefreshTokenRequestRecord request) {
    return service.refreshToken(request);
  }

  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  @PatchMapping("/change-password")
  public void changePassword(
    @Valid @RequestBody ChangePasswordRequestRecord request,
    Principal connectedUser
  ) {
    service.changePassword(request, connectedUser);
  }

  @PutMapping("/update-user")
  public void updateUser(@Valid @RequestBody UpdateUserRequestRecord request, Principal connectedUser) {
    service.updateUser(request, connectedUser);
  }
}
