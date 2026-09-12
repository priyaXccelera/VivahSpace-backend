package com.example.vendorservice.dto;

import com.example.vendorservice.entity.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public class VerificationUpdateRequest {
  @NotNull private VerificationStatus status;

  public VerificationStatus getStatus() {
    return status;
  }

  public void setStatus(VerificationStatus status) {
    this.status = status;
  }
}
