package com.example.vendorservice.controller;

import com.example.vendorservice.dto.CommissionRequest;
import com.example.vendorservice.dto.CommissionResponse;
import com.example.vendorservice.dto.VendorAnalyticsResponse;
import com.example.vendorservice.service.CommissionService;
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
@RequestMapping("/api/v1/vendors/{vendorId}")
@Tag(
    name = "Vendor Commissions & Analytics",
    description = "Commission tracking per booking and vendor performance analytics")
public class CommissionController {

  private final CommissionService commissionService;

  public CommissionController(CommissionService commissionService) {
    this.commissionService = commissionService;
  }

  @PostMapping("/commissions")
  @Operation(summary = "Record a commission earned by a vendor for a booking")
  public ResponseEntity<CommissionResponse> record(
      @PathVariable Long vendorId, @Valid @RequestBody CommissionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commissionService.record(vendorId, request));
  }

  @GetMapping("/commissions")
  @Operation(summary = "List a vendor's commissions, paginated")
  public ResponseEntity<Page<CommissionResponse>> list(
      @PathVariable Long vendorId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(commissionService.list(vendorId, pageable));
  }

  @GetMapping("/analytics")
  @Operation(
      summary = "Vendor performance analytics: total bookings, total commission, average rating")
  public ResponseEntity<VendorAnalyticsResponse> analytics(@PathVariable Long vendorId) {
    return ResponseEntity.ok(commissionService.analytics(vendorId));
  }
}
