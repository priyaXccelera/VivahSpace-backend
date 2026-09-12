package com.example.vendorservice.dto;

import com.example.vendorservice.entity.PortfolioItem;

public class PortfolioItemResponse {
  private Long id;
  private Long vendorId;
  private String title;
  private String imageUrl;
  private String description;

  public static PortfolioItemResponse from(PortfolioItem item) {
    PortfolioItemResponse r = new PortfolioItemResponse();
    r.id = item.getId();
    r.vendorId = item.getVendor().getId();
    r.title = item.getTitle();
    r.imageUrl = item.getImageUrl();
    r.description = item.getDescription();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getVendorId() {
    return vendorId;
  }

  public String getTitle() {
    return title;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getDescription() {
    return description;
  }
}
