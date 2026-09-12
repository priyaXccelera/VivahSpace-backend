package com.example.hallservice.dto;

import com.example.hallservice.entity.Hall;
import com.example.hallservice.entity.VerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class HallResponse {
  private Long id;
  private String ownerName;
  private String name;
  private String location;
  private String description;
  private Integer capacity;
  private BigDecimal basePrice;
  private VerificationStatus verificationStatus;
  private Instant createdAt;
  private Instant updatedAt;

  public static HallResponse from(Hall hall) {
    HallResponse r = new HallResponse();
    r.id = hall.getId();
    r.ownerName = hall.getOwnerName();
    r.name = hall.getName();
    r.location = hall.getLocation();
    r.description = hall.getDescription();
    r.capacity = hall.getCapacity();
    r.basePrice = hall.getBasePrice();
    r.verificationStatus = hall.getVerificationStatus();
    r.createdAt = hall.getCreatedAt();
    r.updatedAt = hall.getUpdatedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public String getDescription() {
    return description;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
