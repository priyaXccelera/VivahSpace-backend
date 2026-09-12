package com.example.vendorservice.controller;

import com.example.vendorservice.dto.PortfolioItemRequest;
import com.example.vendorservice.dto.PortfolioItemResponse;
import com.example.vendorservice.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/portfolio")
@Tag(name = "Vendor Portfolio", description = "Manage a vendor's portfolio items")
public class PortfolioController {

  private final PortfolioService portfolioService;

  public PortfolioController(PortfolioService portfolioService) {
    this.portfolioService = portfolioService;
  }

  @PostMapping
  @Operation(summary = "Add a portfolio item for a vendor")
  public ResponseEntity<PortfolioItemResponse> add(
      @PathVariable Long vendorId, @Valid @RequestBody PortfolioItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(portfolioService.addItem(vendorId, request));
  }

  @GetMapping
  @Operation(summary = "List a vendor's portfolio items, paginated")
  public ResponseEntity<Page<PortfolioItemResponse>> list(
      @PathVariable Long vendorId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(portfolioService.list(vendorId, pageable));
  }

  @DeleteMapping("/{itemId}")
  @Operation(summary = "Delete a vendor's portfolio item")
  public ResponseEntity<Void> delete(@PathVariable Long vendorId, @PathVariable Long itemId) {
    portfolioService.delete(vendorId, itemId);
    return ResponseEntity.noContent().build();
  }
}
