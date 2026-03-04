package android.tester.backend.exceptions;

public class AvdImageNotFoundException extends RuntimeException {
  public AvdImageNotFoundException(String message) {
    super(message);
  }
}
