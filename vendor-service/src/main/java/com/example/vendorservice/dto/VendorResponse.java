package com.example.vendorservice.dto;

import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class VendorResponse {
  private Long id;
  private String ownerName;
  private String businessName;
  private VendorCategory category;
  private String location;
  private String description;
  private BigDecimal basePrice;
  private String contactEmail;
  private String contactPhone;
  private VerificationStatus verificationStatus;
  private BigDecimal rating;
  private Instant createdAt;
  private Instant updatedAt;

  public static VendorResponse from(Vendor vendor) {
    VendorResponse r = new VendorResponse();
    r.id = vendor.getId();
    r.ownerName = vendor.getOwnerName();
    r.businessName = vendor.getBusinessName();
    r.category = vendor.getCategory();
    r.location = vendor.getLocation();
    r.description = vendor.getDescription();
    r.basePrice = vendor.getBasePrice();
    r.contactEmail = vendor.getContactEmail();
    r.contactPhone = vendor.getContactPhone();
    r.verificationStatus = vendor.getVerificationStatus();
    r.rating = vendor.getRating();
    r.createdAt = vendor.getCreatedAt();
    r.updatedAt = vendor.getUpdatedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getBusinessName() {
    return businessName;
  }

  public VendorCategory getCategory() {
    return category;
  }

  public String getLocation() {
    return location;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public BigDecimal getRating() {
    return rating;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
