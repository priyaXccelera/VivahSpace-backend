package com.example.vendorservice.dto;

import java.math.BigDecimal;

public class VendorAnalyticsResponse {
  private Long vendorId;
  private long totalBookings;
  private BigDecimal totalCommission;
  private BigDecimal averageRating;

  public VendorAnalyticsResponse(
      Long vendorId, long totalBookings, BigDecimal totalCommission, BigDecimal averageRating) {
    this.vendorId = vendorId;
    this.totalBookings = totalBookings;
    this.totalCommission = totalCommission;
    this.averageRating = averageRating;
  }

  public Long getVendorId() {
    return vendorId;
  }

  public long getTotalBookings() {
    return totalBookings;
  }

  public BigDecimal getTotalCommission() {
    return totalCommission;
  }

  public BigDecimal getAverageRating() {
    return averageRating;
  }
}
