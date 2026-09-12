package com.example.proposalservice.dto;

import com.example.proposalservice.entity.ProposalItem;
import com.example.proposalservice.entity.ServiceType;
import java.math.BigDecimal;

public class ProposalItemResponse {
  private Long id;
  private Long vendorId;
  private ServiceType serviceType;
  private BigDecimal price;

  public static ProposalItemResponse from(ProposalItem item) {
    ProposalItemResponse r = new ProposalItemResponse();
    r.id = item.getId();
    r.vendorId = item.getVendorId();
    r.serviceType = item.getServiceType();
    r.price = item.getPrice();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getVendorId() {
    return vendorId;
  }

  public ServiceType getServiceType() {
    return serviceType;
  }

  public BigDecimal getPrice() {
    return price;
  }
}
