package com.example.hallservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "hall_availability_slots",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_hall_slot_date_type",
            columnNames = {"hall_id", "slot_date", "slot_type"}))
public class HallAvailabilitySlot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hall_id", nullable = false)
  private Hall hall;

  @Column(name = "slot_date", nullable = false)
  private LocalDate slotDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "slot_type", nullable = false, length = 20)
  private SlotType slotType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SlotStatus status = SlotStatus.AVAILABLE;

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

  public SlotStatus getStatus() {
    return status;
  }

  public void setStatus(SlotStatus status) {
    this.status = status;
  }
}
