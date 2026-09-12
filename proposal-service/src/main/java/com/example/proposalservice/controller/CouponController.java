package com.example.proposalservice.controller;

import com.example.proposalservice.dto.CouponRequest;
import com.example.proposalservice.dto.CouponResponse;
import com.example.proposalservice.service.CouponService;
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
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Coupon codes applied to proposal totals")
public class CouponController {

  private final CouponService service;

  public CouponController(CouponService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Create a coupon")
  public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping
  @Operation(summary = "List coupons, paginated")
  public ResponseEntity<Page<CouponResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(service.list(pageable));
  }
}
