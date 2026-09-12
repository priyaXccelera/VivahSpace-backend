package com.example.hallservice.dto;

import com.example.hallservice.entity.HallAvailabilitySlot;
import com.example.hallservice.entity.SlotStatus;
import com.example.hallservice.entity.SlotType;
import java.time.LocalDate;

public class SlotResponse {
  private Long id;
  private Long hallId;
  private String hallName;
  private LocalDate slotDate;
  private SlotType slotType;
  private SlotStatus status;

  public static SlotResponse from(HallAvailabilitySlot slot) {
    SlotResponse r = new SlotResponse();
    r.id = slot.getId();
    r.hallId = slot.getHall().getId();
    r.hallName = slot.getHall().getName();
    r.slotDate = slot.getSlotDate();
    r.slotType = slot.getSlotType();
    r.status = slot.getStatus();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getHallId() {
    return hallId;
  }

  public String getHallName() {
    return hallName;
  }

  public LocalDate getSlotDate() {
    return slotDate;
  }

  public SlotType getSlotType() {
    return slotType;
  }

  public SlotStatus getStatus() {
    return status;
  }
}
