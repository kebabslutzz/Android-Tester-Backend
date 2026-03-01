package android.tester.backend.dtos.user;

public record AuthenticationResponseRecord(
  String token,
  String refreshToken,
  String username,
  String user
) {
}
