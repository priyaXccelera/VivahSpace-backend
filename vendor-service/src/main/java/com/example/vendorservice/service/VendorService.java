package com.example.vendorservice.service;

import com.example.vendorservice.dto.RatingUpdateRequest;
import com.example.vendorservice.dto.VendorRequest;
import com.example.vendorservice.dto.VendorResponse;
import com.example.vendorservice.dto.VerificationUpdateRequest;
import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

  private final VendorRepository vendorRepository;

  public VendorService(VendorRepository vendorRepository) {
    this.vendorRepository = vendorRepository;
  }

  @Transactional
  public VendorResponse create(VendorRequest request) {
    Vendor vendor = new Vendor();
    apply(vendor, request);
    return VendorResponse.from(vendorRepository.save(vendor));
  }

  @Transactional(readOnly = true)
  public Page<VendorResponse> list(
      VendorCategory category, VerificationStatus status, Pageable pageable) {
    Page<Vendor> page;
    if (category != null && status != null) {
      page = vendorRepository.findByCategoryAndVerificationStatus(category, status, pageable);
    } else if (category != null) {
      page = vendorRepository.findByCategory(category, pageable);
    } else if (status != null) {
      page = vendorRepository.findByVerificationStatus(status, pageable);
    } else {
      page = vendorRepository.findAll(pageable);
    }
    return page.map(VendorResponse::from);
  }

  @Transactional(readOnly = true)
  public VendorResponse getById(Long id) {
    return VendorResponse.from(findVendor(id));
  }

  @Transactional
  public VendorResponse update(Long id, VendorRequest request) {
    Vendor vendor = findVendor(id);
    apply(vendor, request);
    return VendorResponse.from(vendorRepository.save(vendor));
  }

  @Transactional
  public void delete(Long id) {
    Vendor vendor = findVendor(id);
    vendorRepository.delete(vendor);
  }

  @Transactional
  public VendorResponse updateVerification(Long id, VerificationUpdateRequest request) {
    Vendor vendor = findVendor(id);
    vendor.setVerificationStatus(request.getStatus());
    return VendorResponse.from(vendorRepository.save(vendor));
  }

  @Transactional
  public VendorResponse updateRating(Long id, RatingUpdateRequest request) {
    Vendor vendor = findVendor(id);
    vendor.setRating(request.getRating());
    return VendorResponse.from(vendorRepository.save(vendor));
  }

  public Vendor findVendor(Long id) {
    return vendorRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id " + id));
  }

  private void apply(Vendor vendor, VendorRequest request) {
    vendor.setOwnerName(request.getOwnerName());
    vendor.setBusinessName(request.getBusinessName());
    vendor.setCategory(request.getCategory());
    vendor.setLocation(request.getLocation());
    vendor.setDescription(request.getDescription());
    vendor.setBasePrice(request.getBasePrice());
    vendor.setContactEmail(request.getContactEmail());
    vendor.setContactPhone(request.getContactPhone());
  }
}
