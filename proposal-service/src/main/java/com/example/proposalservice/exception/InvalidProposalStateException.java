package com.example.proposalservice.exception;

public class InvalidProposalStateException extends RuntimeException {
  public InvalidProposalStateException(String message) {
    super(message);
  }
}
