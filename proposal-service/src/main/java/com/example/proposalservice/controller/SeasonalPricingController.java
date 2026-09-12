package com.example.proposalservice.controller;

import com.example.proposalservice.dto.SeasonalPricingRequest;
import com.example.proposalservice.dto.SeasonalPricingResponse;
import com.example.proposalservice.service.SeasonalPricingService;
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
@RequestMapping("/api/v1/seasonal-pricing")
@Tag(
    name = "Seasonal Pricing",
    description = "Dynamic seasonal pricing rules applied to proposal totals")
public class SeasonalPricingController {

  private final SeasonalPricingService service;

  public SeasonalPricingController(SeasonalPricingService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Create a seasonal pricing rule")
  public ResponseEntity<SeasonalPricingResponse> create(
      @Valid @RequestBody SeasonalPricingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping
  @Operation(summary = "List seasonal pricing rules, paginated")
  public ResponseEntity<Page<SeasonalPricingResponse>> list(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(service.list(pageable));
  }
}
