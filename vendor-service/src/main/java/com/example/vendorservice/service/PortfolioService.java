package com.example.vendorservice.service;

import com.example.vendorservice.dto.PortfolioItemRequest;
import com.example.vendorservice.dto.PortfolioItemResponse;
import com.example.vendorservice.entity.PortfolioItem;
import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.repository.PortfolioItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

  private final PortfolioItemRepository portfolioItemRepository;
  private final VendorService vendorService;

  public PortfolioService(
      PortfolioItemRepository portfolioItemRepository, VendorService vendorService) {
    this.portfolioItemRepository = portfolioItemRepository;
    this.vendorService = vendorService;
  }

  @Transactional
  public PortfolioItemResponse addItem(Long vendorId, PortfolioItemRequest request) {
    Vendor vendor = vendorService.findVendor(vendorId);
    PortfolioItem item = new PortfolioItem();
    item.setVendor(vendor);
    item.setTitle(request.getTitle());
    item.setImageUrl(request.getImageUrl());
    item.setDescription(request.getDescription());
    return PortfolioItemResponse.from(portfolioItemRepository.save(item));
  }

  @Transactional(readOnly = true)
  public Page<PortfolioItemResponse> list(Long vendorId, Pageable pageable) {
    vendorService.findVendor(vendorId);
    return portfolioItemRepository
        .findByVendorId(vendorId, pageable)
        .map(PortfolioItemResponse::from);
  }

  @Transactional
  public void delete(Long vendorId, Long itemId) {
    PortfolioItem item =
        portfolioItemRepository
            .findById(itemId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Portfolio item not found with id " + itemId));
    if (!item.getVendor().getId().equals(vendorId)) {
      throw new ResourceNotFoundException(
          "Portfolio item " + itemId + " does not belong to vendor " + vendorId);
    }
    portfolioItemRepository.delete(item);
  }
}
