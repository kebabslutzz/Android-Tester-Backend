package android.tester.backend.handlers;

import android.tester.backend.dtos.ErrorResponseRecord;
import android.tester.backend.exceptions.ConflictException;
import android.tester.backend.exceptions.NotFoundException;
import android.tester.backend.exceptions.ValidationException;
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

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<List<ErrorResponseRecord>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    List<ErrorResponseRecord> errors = ex.getBindingResult().getFieldErrors().stream()
      .map(fieldError -> new ErrorResponseRecord(
        fieldError.getField() + ": " + fieldError.getDefaultMessage(),
        HttpStatus.BAD_REQUEST.value()
      ))
      .collect(Collectors.toList());
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  // Uses standard ValidationException or custom ones if you create them
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponseRecord> handleValidationException(ValidationException ex) {
    ErrorResponseRecord error = new ErrorResponseRecord(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseRecord> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    ErrorResponseRecord error = new ErrorResponseRecord("Invalid request body", HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
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
}
