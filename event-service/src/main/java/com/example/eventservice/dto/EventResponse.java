package com.example.eventservice.dto;

import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.EventStatus;
import java.time.Instant;

public class EventResponse {
  private Long id;
  private Long proposalId;
  private Long hallId;
  private Long userId;
  private Integer guestCount;
  private EventStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  public static EventResponse from(Event e) {
    EventResponse r = new EventResponse();
    r.id = e.getId();
    r.proposalId = e.getProposalId();
    r.hallId = e.getHallId();
    r.userId = e.getUserId();
    r.guestCount = e.getGuestCount();
    r.status = e.getStatus();
    r.createdAt = e.getCreatedAt();
    r.updatedAt = e.getUpdatedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getProposalId() {
    return proposalId;
  }

  public Long getHallId() {
    return hallId;
  }

  public Long getUserId() {
    return userId;
  }

  public Integer getGuestCount() {
    return guestCount;
  }

  public EventStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
