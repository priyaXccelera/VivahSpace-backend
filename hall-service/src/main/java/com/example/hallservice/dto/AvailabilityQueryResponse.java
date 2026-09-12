package com.example.hallservice.dto;

import java.util.List;

public class AvailabilityQueryResponse {
  private Long hallId;
  private List<SlotResponse> availableSlots;
  private List<HallResponse> suggestedAlternatives;

  public AvailabilityQueryResponse(
      Long hallId, List<SlotResponse> availableSlots, List<HallResponse> suggestedAlternatives) {
    this.hallId = hallId;
    this.availableSlots = availableSlots;
    this.suggestedAlternatives = suggestedAlternatives;
  }

  public Long getHallId() {
    return hallId;
  }

  public List<SlotResponse> getAvailableSlots() {
    return availableSlots;
  }

  public List<HallResponse> getSuggestedAlternatives() {
    return suggestedAlternatives;
  }
}
