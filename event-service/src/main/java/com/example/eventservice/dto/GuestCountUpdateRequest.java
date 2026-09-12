package com.example.eventservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class GuestCountUpdateRequest {
  @NotNull @Positive private Integer guestCount;

  public Integer getGuestCount() {
    return guestCount;
  }

  public void setGuestCount(Integer guestCount) {
    this.guestCount = guestCount;
  }
}
