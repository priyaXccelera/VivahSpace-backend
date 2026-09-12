package com.example.proposalservice.dto;

import com.example.proposalservice.entity.Coupon;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CouponResponse {
  private Long id;
  private String code;
  private BigDecimal discountPercent;
  private boolean active;
  private LocalDate expiryDate;

  public static CouponResponse from(Coupon c) {
    CouponResponse r = new CouponResponse();
    r.id = c.getId();
    r.code = c.getCode();
    r.discountPercent = c.getDiscountPercent();
    r.active = c.isActive();
    r.expiryDate = c.getExpiryDate();
    return r;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public BigDecimal getDiscountPercent() {
    return discountPercent;
  }

  public boolean isActive() {
    return active;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }
}
