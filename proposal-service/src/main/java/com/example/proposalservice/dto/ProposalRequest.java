package com.example.proposalservice.dto;

import jakarta.validation.constraints.NotNull;

public class ProposalRequest {
  @NotNull private Long userId;

  @NotNull private Long hallId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getHallId() {
    return hallId;
  }

  public void setHallId(Long hallId) {
    this.hallId = hallId;
  }
}
