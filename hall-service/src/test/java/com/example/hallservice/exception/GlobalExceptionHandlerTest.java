package com.example.hallservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Mock private MethodArgumentNotValidException methodArgumentNotValidException;

  @Mock private BindingResult bindingResult;

  @Test
  void handleNotFound_returns404WithMessage() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleNotFound(new ResourceNotFoundException("Hall not found with id 1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message")).isEqualTo("Hall not found with id 1");
    assertThat(response.getBody().get("status")).isEqualTo(404);
    assertThat(response.getBody().get("error")).isEqualTo("Not Found");
    assertThat(response.getBody()).containsKey("timestamp");
  }

  @Test
  void handleConflict_returns409WithMessage() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleConflict(new SlotUnavailableException("Slot is booked"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().get("message")).isEqualTo("Slot is booked");
  }

  @Test
  void handleValidation_returns400WithFirstFieldError() {
    FieldError fieldError = new FieldError("hallRequest", "name", "must not be blank");
    when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<Map<String, Object>> response =
        handler.handleValidation(methodArgumentNotValidException);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().get("message")).isEqualTo("name: must not be blank");
  }

  @Test
  void handleValidation_noFieldErrors_usesDefaultMessage() {
    when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of());

    ResponseEntity<Map<String, Object>> response =
        handler.handleValidation(methodArgumentNotValidException);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().get("message")).isEqualTo("Validation failed");
  }

  @Test
  void handleIllegalArgument_returns400WithMessage() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().get("message")).isEqualTo("bad input");
  }
}
