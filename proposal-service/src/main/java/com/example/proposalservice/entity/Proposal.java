package com.example.proposalservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposals")
public class Proposal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long hallId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal hallBasePrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProposalStatus status = ProposalStatus.DRAFT;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal estimatedTotal = BigDecimal.ZERO;

  @Column(length = 40)
  private String couponCode;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProposalItem> items = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.status == null) {
      this.status = ProposalStatus.DRAFT;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getHallId() {
    return hallId;
  }

  public void setHallId(Long hallId) {
    this.hallId = hallId;
  }

  public BigDecimal getHallBasePrice() {
    return hallBasePrice;
  }

  public void setHallBasePrice(BigDecimal hallBasePrice) {
    this.hallBasePrice = hallBasePrice;
  }

  public ProposalStatus getStatus() {
    return status;
  }

  public void setStatus(ProposalStatus status) {
    this.status = status;
  }

  public BigDecimal getEstimatedTotal() {
    return estimatedTotal;
  }

  public void setEstimatedTotal(BigDecimal estimatedTotal) {
    this.estimatedTotal = estimatedTotal;
  }

  public String getCouponCode() {
    return couponCode;
  }

  public void setCouponCode(String couponCode) {
    this.couponCode = couponCode;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<ProposalItem> getItems() {
    return items;
  }

  public void setItems(List<ProposalItem> items) {
    this.items = items;
  }
}
