package android.tester.backend.dtos.user;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequestRecord(
  @NotNull String refreshToken
) {
}
