package com.example.hallservice.dto;

import jakarta.validation.constraints.NotNull;

public class WaitlistRequest {
  @NotNull private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
