package android.tester.backend.dtos;

public record ErrorResponseRecord(
  String message,
  int status
) {
}
