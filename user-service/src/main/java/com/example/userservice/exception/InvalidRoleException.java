package com.example.userservice.exception;

public class InvalidRoleException extends RuntimeException {

  public InvalidRoleException(String message) {
    super(message);
  }
}
