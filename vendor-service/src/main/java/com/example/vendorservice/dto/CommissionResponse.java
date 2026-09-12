package com.example.vendorservice.dto;

import com.example.vendorservice.entity.Commission;
import java.math.BigDecimal;
import java.time.Instant;

public class CommissionResponse {
  private Long id;
  private Long vendorId;
  private Long bookingId;
  private BigDecimal amount;
  private Instant createdAt;

  public static CommissionResponse from(Commission c) {
    CommissionResponse r = new CommissionResponse();
    r.id = c.getId();
    r.vendorId = c.getVendor().getId();
    r.bookingId = c.getBookingId();
    r.amount = c.getAmount();
    r.createdAt = c.getCreatedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getVendorId() {
    return vendorId;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
