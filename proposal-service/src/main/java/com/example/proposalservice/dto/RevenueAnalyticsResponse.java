package com.example.proposalservice.dto;

import java.math.BigDecimal;

public class RevenueAnalyticsResponse {
  private long confirmedBookings;
  private BigDecimal totalRevenue;
  private BigDecimal hallRevenue;
  private BigDecimal vendorRevenue;

  public RevenueAnalyticsResponse(
      long confirmedBookings,
      BigDecimal totalRevenue,
      BigDecimal hallRevenue,
      BigDecimal vendorRevenue) {
    this.confirmedBookings = confirmedBookings;
    this.totalRevenue = totalRevenue;
    this.hallRevenue = hallRevenue;
    this.vendorRevenue = vendorRevenue;
  }

  public long getConfirmedBookings() {
    return confirmedBookings;
  }

  public BigDecimal getTotalRevenue() {
    return totalRevenue;
  }

  public BigDecimal getHallRevenue() {
    return hallRevenue;
  }

  public BigDecimal getVendorRevenue() {
    return vendorRevenue;
  }
}
