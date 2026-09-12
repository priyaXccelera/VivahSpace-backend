package com.example.vendorservice.service;

import com.example.vendorservice.dto.CommissionRequest;
import com.example.vendorservice.dto.CommissionResponse;
import com.example.vendorservice.dto.VendorAnalyticsResponse;
import com.example.vendorservice.entity.Commission;
import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.repository.CommissionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {

  private final CommissionRepository commissionRepository;
  private final VendorService vendorService;

  public CommissionService(CommissionRepository commissionRepository, VendorService vendorService) {
    this.commissionRepository = commissionRepository;
    this.vendorService = vendorService;
  }

  @Transactional
  public CommissionResponse record(Long vendorId, CommissionRequest request) {
    Vendor vendor = vendorService.findVendor(vendorId);
    Commission commission = new Commission();
    commission.setVendor(vendor);
    commission.setBookingId(request.getBookingId());
    commission.setAmount(request.getAmount());
    return CommissionResponse.from(commissionRepository.save(commission));
  }

  @Transactional(readOnly = true)
  public Page<CommissionResponse> list(Long vendorId, Pageable pageable) {
    vendorService.findVendor(vendorId);
    return commissionRepository.findByVendorId(vendorId, pageable).map(CommissionResponse::from);
  }

  @Transactional(readOnly = true)
  public VendorAnalyticsResponse analytics(Long vendorId) {
    Vendor vendor = vendorService.findVendor(vendorId);
    List<Commission> commissions = commissionRepository.findByVendorId(vendorId);
    BigDecimal total =
        commissions.stream().map(Commission::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new VendorAnalyticsResponse(vendorId, commissions.size(), total, vendor.getRating());
  }
}
