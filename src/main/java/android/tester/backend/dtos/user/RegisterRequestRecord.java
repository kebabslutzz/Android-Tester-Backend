package android.tester.backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestRecord(
  @NotNull(message = "Name must be provided")
  @Size(min = 4, max = 64, message = "Name must between {min} and {max} characters long")
  String name,

  @NotNull(message = "Password must be provided")
  @Size(min = 8, max = 64, message = "Password must between {min} and {max} characters long")
  @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,64}$", message = "Password must contain at least 1 special symbol, digit, and capital letter")
  String password,

  @NotNull(message = "Email must be provided")
  @Email(message = "Email must be valid")
  String email
) {
}
