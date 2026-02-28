package android.tester.backend.exceptions;

public class FileStorageException extends RuntimeException {
  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
