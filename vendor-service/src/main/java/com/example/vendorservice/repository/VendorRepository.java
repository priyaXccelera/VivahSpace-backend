package com.example.vendorservice.repository;

import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

  Page<Vendor> findByVerificationStatus(VerificationStatus status, Pageable pageable);

  Page<Vendor> findByCategory(VendorCategory category, Pageable pageable);

  Page<Vendor> findByCategoryAndVerificationStatus(
      VendorCategory category, VerificationStatus status, Pageable pageable);
}
