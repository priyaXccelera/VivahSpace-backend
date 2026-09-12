package com.example.vendorservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendors")
public class Vendor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String ownerName;

  @Column(nullable = false, length = 150)
  private String businessName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private VendorCategory category;

  @Column(nullable = false, length = 200)
  private String location;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal basePrice;

  @Column(nullable = false, length = 150)
  private String contactEmail;

  @Column(nullable = false, length = 20)
  private String contactPhone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal rating = BigDecimal.ZERO;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PortfolioItem> portfolioItems = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.verificationStatus == null) {
      this.verificationStatus = VerificationStatus.UNVERIFIED;
    }
    if (this.rating == null) {
      this.rating = BigDecimal.ZERO;
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

  public String getBusinessName() {
    return businessName;
  }

  public void setBusinessName(String businessName) {
    this.businessName = businessName;
  }

  public VendorCategory getCategory() {
    return category;
  }

  public void setCategory(VendorCategory category) {
    this.category = category;
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

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(VerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public BigDecimal getRating() {
    return rating;
  }

  public void setRating(BigDecimal rating) {
    this.rating = rating;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<PortfolioItem> getPortfolioItems() {
    return portfolioItems;
  }

  public void setPortfolioItems(List<PortfolioItem> portfolioItems) {
    this.portfolioItems = portfolioItems;
  }
}
