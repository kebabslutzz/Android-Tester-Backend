package android.tester.backend.dtos.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestRecord(
  @NotNull(message = "Current password is required")
  String currentPassword,

  @NotNull(message = "New password is required")
  @Size(min = 8, max = 64, message = "Password must between {min} and {max} characters long")
  @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,64}$", message = "Password must contain at least 1 special symbol, digit, and capital letter")
  String newPassword,

  @NotNull(message = "Confirmation password is required")
  String confirmationPassword
) {
}
