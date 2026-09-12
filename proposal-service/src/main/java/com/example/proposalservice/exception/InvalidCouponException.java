package com.example.proposalservice.exception;

public class InvalidCouponException extends RuntimeException {
  public InvalidCouponException(String message) {
    super(message);
  }
}
