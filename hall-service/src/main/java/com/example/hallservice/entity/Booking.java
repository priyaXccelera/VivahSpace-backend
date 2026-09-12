package com.example.hallservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

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

  @Column private Long proposalId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status = BookingStatus.CONFIRMED;

  @Column(nullable = false, updatable = false)
  private Instant bookedAt;

  @PrePersist
  protected void onCreate() {
    this.bookedAt = Instant.now();
    if (this.status == null) {
      this.status = BookingStatus.CONFIRMED;
    }
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

  public Long getProposalId() {
    return proposalId;
  }

  public void setProposalId(Long proposalId) {
    this.proposalId = proposalId;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public Instant getBookedAt() {
    return bookedAt;
  }
}
