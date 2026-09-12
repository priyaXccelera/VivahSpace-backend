package com.example.hallservice.dto;

import com.example.hallservice.entity.SlotType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class SlotRequest {
  @NotNull private LocalDate slotDate;

  @NotNull private SlotType slotType;

  public LocalDate getSlotDate() {
    return slotDate;
  }

  public void setSlotDate(LocalDate slotDate) {
    this.slotDate = slotDate;
  }

  public SlotType getSlotType() {
    return slotType;
  }

  public void setSlotType(SlotType slotType) {
    this.slotType = slotType;
  }
}
