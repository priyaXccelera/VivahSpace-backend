package com.example.proposalservice.dto;

import com.example.proposalservice.entity.SeasonalPricingRule;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SeasonalPricingResponse {
  private Long id;
  private String name;
  private LocalDate startDate;
  private LocalDate endDate;
  private BigDecimal multiplier;

  public static SeasonalPricingResponse from(SeasonalPricingRule rule) {
    SeasonalPricingResponse r = new SeasonalPricingResponse();
    r.id = rule.getId();
    r.name = rule.getName();
    r.startDate = rule.getStartDate();
    r.endDate = rule.getEndDate();
    r.multiplier = rule.getMultiplier();
    return r;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public BigDecimal getMultiplier() {
    return multiplier;
  }
}
