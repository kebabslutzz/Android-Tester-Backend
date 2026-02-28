package android.tester.backend.dtos.user;

public record AuthenticationResponseRecord(
  String token,
  String username,
  String user
) {
}
