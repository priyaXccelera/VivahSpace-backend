package com.example.userservice.exception;

import com.example.userservice.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> handleEmailExists(
      EmailAlreadyExistsException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
  }

  @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
  public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
      RuntimeException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request, null);
  }

  @ExceptionHandler(InvalidRoleException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidRole(
      InvalidRoleException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return build(
        HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
  }

  private ResponseEntity<ApiErrorResponse> build(
      HttpStatus status, String message, HttpServletRequest request, List<String> details) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            status.value(), status.getReasonPhrase(), message, request.getRequestURI(), details);
    return ResponseEntity.status(status).body(body);
  }
}
