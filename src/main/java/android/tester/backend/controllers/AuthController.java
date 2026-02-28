package android.tester.backend.controllers;

import android.tester.backend.dtos.user.AuthenticationRequestRecord;
import android.tester.backend.dtos.user.AuthenticationResponseRecord;
import android.tester.backend.dtos.user.RegisterRequestRecord;
import android.tester.backend.services.auth.AuthenticationService;
import android.tester.backend.services.logging.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthenticationService service;
  private final LogService logger;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponseRecord> register(@Valid @RequestBody RegisterRequestRecord request) {
    logger.addLog("request received: " + request.toString());
    return ResponseEntity.ok(service.register(request));
  }

  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponseRecord> authenticate(@Valid @RequestBody AuthenticationRequestRecord request) {
    return ResponseEntity.ok(service.authenticate(request));
  }
}
