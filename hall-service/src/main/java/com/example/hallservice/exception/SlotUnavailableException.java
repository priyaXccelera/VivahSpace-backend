package com.example.hallservice.exception;

public class SlotUnavailableException extends RuntimeException {
  public SlotUnavailableException(String message) {
    super(message);
  }
}
