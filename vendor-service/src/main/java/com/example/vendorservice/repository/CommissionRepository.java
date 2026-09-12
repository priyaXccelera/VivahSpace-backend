package com.example.vendorservice.repository;

import com.example.vendorservice.entity.Commission;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRepository extends JpaRepository<Commission, Long> {
  Page<Commission> findByVendorId(Long vendorId, Pageable pageable);

  List<Commission> findByVendorId(Long vendorId);
}
