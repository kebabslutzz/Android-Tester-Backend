package android.tester.backend.handlers;

import android.tester.backend.dtos.ErrorResponseRecord;
import android.tester.backend.exceptions.*;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponseRecord handleValidationExceptions(MethodArgumentNotValidException ex) {
    // Concatenate all validation errors into a single string for simple clients
    // OR create a specialized ErrorResponseRecord that takes a list
    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
      .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
      .collect(Collectors.joining(", "));

    return new ErrorResponseRecord(errorMessage, HttpStatus.BAD_REQUEST.value());
  }

  // Uses standard ValidationException or custom ones if you create them
  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponseRecord handleValidationException(ValidationException ex) {
    return new ErrorResponseRecord(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponseRecord handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    return new ErrorResponseRecord("Invalid request body", HttpStatus.BAD_REQUEST.value());
  }

  @ExceptionHandler(BadCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponseRecord handleBadCredentialsException(BadCredentialsException ex) {
    return new ErrorResponseRecord("Invalid email or password", HttpStatus.UNAUTHORIZED.value());
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponseRecord handleAccessDeniedException(AccessDeniedException ex) {
    return new ErrorResponseRecord("Access Denied", HttpStatus.FORBIDDEN.value());
  }

  @ExceptionHandler({AuthenticationException.class, ExpiredJwtException.class, SignatureException.class})
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponseRecord handleSecurityExceptions(Exception ex) {
    String message = "Access denied";
    if (ex instanceof ExpiredJwtException) {
      message = "JWT token has expired";
    } else if (ex instanceof SignatureException) {
      message = "Invalid JWT signature";
    }
    return new ErrorResponseRecord(message, HttpStatus.FORBIDDEN.value());
  }

  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponseRecord handleConflictException(ConflictException ex) {
    return new ErrorResponseRecord(ex.getMessage(), HttpStatus.CONFLICT.value());
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ErrorResponseRecord handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
    return new ErrorResponseRecord(
      "The resource was updated by another request. Please try again.",
      HttpStatus.CONFLICT.value()
    );
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponseRecord handleNotFoundException(NotFoundException ex) {
    return new ErrorResponseRecord(ex.getMessage(), HttpStatus.NOT_FOUND.value());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponseRecord handleInternalServerError(Exception ex) {
    // Log real error here
    ex.printStackTrace();
    return new ErrorResponseRecord("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @ExceptionHandler(AvdCreationException.class)
  public ResponseEntity<Object> handleAvdCreationException(AvdCreationException ex) {
    return ResponseEntity.status(601).body(Map.of("error", "AVD Creation Failed", "message", ex.getMessage()));
  }

  @ExceptionHandler(DeviceBootException.class)
  public ResponseEntity<Object> handleDeviceBootException(DeviceBootException ex) {
    return ResponseEntity.status(602).body(Map.of("error", "AVD Boot Failed", "message", ex.getMessage()));
  }

  @ExceptionHandler(TestExecutionException.class)
  public ResponseEntity<Object> handleTestExecutionException(TestExecutionException ex) {
    return ResponseEntity.status(603).body(Map.of("error", "Test Execution Failed", "message", ex.getMessage()));
  }

  @ExceptionHandler(AvdImageNotFoundException.class)
  public ResponseEntity<Object> handleAvdImageNotFoundException(AvdImageNotFoundException ex) {
    return ResponseEntity.status(604).body(Map.of("error", "Invalid System Image", "message", ex.getMessage()));
  }

  @ExceptionHandler(InvalidApiLevelException.class)
  public ResponseEntity<Object> handleInvalidApiLevelException(InvalidApiLevelException ex) {
    return ResponseEntity.status(605).body(Map.of("error", "Invalid API Level", "message", ex.getMessage()));
  }

}
