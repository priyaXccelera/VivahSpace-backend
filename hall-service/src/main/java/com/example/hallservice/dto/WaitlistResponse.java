package com.example.hallservice.dto;

import com.example.hallservice.entity.Waitlist;
import java.time.Instant;

public class WaitlistResponse {
  private Long id;
  private Long hallId;
  private Long slotId;
  private Long userId;
  private boolean notified;
  private Instant joinedAt;

  public static WaitlistResponse from(Waitlist w) {
    WaitlistResponse r = new WaitlistResponse();
    r.id = w.getId();
    r.hallId = w.getHall().getId();
    r.slotId = w.getSlot().getId();
    r.userId = w.getUserId();
    r.notified = w.isNotified();
    r.joinedAt = w.getJoinedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getHallId() {
    return hallId;
  }

  public Long getSlotId() {
    return slotId;
  }

  public Long getUserId() {
    return userId;
  }

  public boolean isNotified() {
    return notified;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }
}
