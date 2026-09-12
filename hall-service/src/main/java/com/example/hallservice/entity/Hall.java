package com.example.hallservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "halls")
public class Hall {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String ownerName;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 200)
  private String location;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false)
  private Integer capacity;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal basePrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HallAvailabilitySlot> slots = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.verificationStatus == null) {
      this.verificationStatus = VerificationStatus.UNVERIFIED;
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

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(VerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<HallAvailabilitySlot> getSlots() {
    return slots;
  }

  public void setSlots(List<HallAvailabilitySlot> slots) {
    this.slots = slots;
  }
}
