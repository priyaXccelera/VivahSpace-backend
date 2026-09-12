package com.example.proposalservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CouponApplyRequest {
  @NotBlank private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
