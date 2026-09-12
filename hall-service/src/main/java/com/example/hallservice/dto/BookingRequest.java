package com.example.hallservice.dto;

import jakarta.validation.constraints.NotNull;

public class BookingRequest {
  @NotNull private Long userId;

  private Long proposalId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getProposalId() {
    return proposalId;
  }

  public void setProposalId(Long proposalId) {
    this.proposalId = proposalId;
  }
}
