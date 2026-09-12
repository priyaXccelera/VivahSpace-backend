package com.example.hallservice.dto;

import com.example.hallservice.entity.Booking;
import com.example.hallservice.entity.BookingStatus;
import java.time.Instant;

public class BookingResponse {
  private Long id;
  private Long hallId;
  private Long slotId;
  private Long userId;
  private Long proposalId;
  private BookingStatus status;
  private Instant bookedAt;

  public static BookingResponse from(Booking booking) {
    BookingResponse r = new BookingResponse();
    r.id = booking.getId();
    r.hallId = booking.getHall().getId();
    r.slotId = booking.getSlot().getId();
    r.userId = booking.getUserId();
    r.proposalId = booking.getProposalId();
    r.status = booking.getStatus();
    r.bookedAt = booking.getBookedAt();
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

  public Long getProposalId() {
    return proposalId;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public Instant getBookedAt() {
    return bookedAt;
  }
}
