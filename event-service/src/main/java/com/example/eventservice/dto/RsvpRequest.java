package com.example.eventservice.dto;

import com.example.eventservice.entity.RsvpStatus;
import jakarta.validation.constraints.NotBlank;

public class RsvpRequest {
  @NotBlank private String guestName;
  private String contactInfo;
  private RsvpStatus status;

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public String getContactInfo() {
    return contactInfo;
  }

  public void setContactInfo(String contactInfo) {
    this.contactInfo = contactInfo;
  }

  public RsvpStatus getStatus() {
    return status;
  }

  public void setStatus(RsvpStatus status) {
    this.status = status;
  }
}
