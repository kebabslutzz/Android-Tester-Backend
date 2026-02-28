package android.tester.backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthenticationRequestRecord(
  @NotNull @Size(min = 8, max = 64) @Pattern(regexp = "^(?=.*\\d)(?=.*[!@#$%^&*]).*$")
  String password,
  @NotNull @Email
  String email
) {
}
