package com.example.vendorservice.repository;

import com.example.vendorservice.entity.PortfolioItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
  Page<PortfolioItem> findByVendorId(Long vendorId, Pageable pageable);
}
