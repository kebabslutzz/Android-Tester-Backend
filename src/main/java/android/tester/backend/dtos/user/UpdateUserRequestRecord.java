package android.tester.backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestRecord(
  @NotNull(message = "Name must be provided")
  @Size(min = 4, max = 64, message = "Name must between {min} and {max} characters long")
  String name,

  @NotNull(message = "Email must be provided")
  @Email(message = "Email must be valid")
  String email
) {
}
