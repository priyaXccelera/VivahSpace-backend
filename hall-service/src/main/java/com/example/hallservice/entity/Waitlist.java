package com.example.hallservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "waitlist_entries")
public class Waitlist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hall_id", nullable = false)
  private Hall hall;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "slot_id", nullable = false)
  private HallAvailabilitySlot slot;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private boolean notified = false;

  @Column(nullable = false, updatable = false)
  private Instant joinedAt;

  @PrePersist
  protected void onCreate() {
    this.joinedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Hall getHall() {
    return hall;
  }

  public void setHall(Hall hall) {
    this.hall = hall;
  }

  public HallAvailabilitySlot getSlot() {
    return slot;
  }

  public void setSlot(HallAvailabilitySlot slot) {
    this.slot = slot;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public boolean isNotified() {
    return notified;
  }

  public void setNotified(boolean notified) {
    this.notified = notified;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }
}
