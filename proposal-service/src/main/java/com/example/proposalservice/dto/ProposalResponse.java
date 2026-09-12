package com.example.proposalservice.dto;

import com.example.proposalservice.entity.Proposal;
import com.example.proposalservice.entity.ProposalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ProposalResponse {
  private Long id;
  private Long userId;
  private Long hallId;
  private BigDecimal hallBasePrice;
  private ProposalStatus status;
  private BigDecimal estimatedTotal;
  private String couponCode;
  private List<ProposalItemResponse> items;
  private Instant createdAt;
  private Instant updatedAt;

  public static ProposalResponse from(Proposal p) {
    ProposalResponse r = new ProposalResponse();
    r.id = p.getId();
    r.userId = p.getUserId();
    r.hallId = p.getHallId();
    r.hallBasePrice = p.getHallBasePrice();
    r.status = p.getStatus();
    r.estimatedTotal = p.getEstimatedTotal();
    r.couponCode = p.getCouponCode();
    r.items = p.getItems().stream().map(ProposalItemResponse::from).collect(Collectors.toList());
    r.createdAt = p.getCreatedAt();
    r.updatedAt = p.getUpdatedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getHallId() {
    return hallId;
  }

  public BigDecimal getHallBasePrice() {
    return hallBasePrice;
  }

  public ProposalStatus getStatus() {
    return status;
  }

  public BigDecimal getEstimatedTotal() {
    return estimatedTotal;
  }

  public String getCouponCode() {
    return couponCode;
  }

  public List<ProposalItemResponse> getItems() {
    return items;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
