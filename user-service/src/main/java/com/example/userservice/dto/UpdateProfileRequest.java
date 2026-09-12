package com.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

  @NotBlank(message = "fullName is required")
  @Size(max = 120, message = "fullName must be at most 120 characters")
  private String fullName;

  @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "phoneNumber is invalid")
  private String phoneNumber;

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
}
