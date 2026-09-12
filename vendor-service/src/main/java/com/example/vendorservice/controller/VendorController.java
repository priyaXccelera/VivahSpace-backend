package com.example.vendorservice.controller;

import com.example.vendorservice.dto.RatingUpdateRequest;
import com.example.vendorservice.dto.VendorRequest;
import com.example.vendorservice.dto.VendorResponse;
import com.example.vendorservice.dto.VerificationUpdateRequest;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import com.example.vendorservice.service.VendorService;
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
@RequestMapping("/api/v1/vendors")
@Tag(
    name = "Vendors",
    description =
        "Manage wedding vendors (photographers, caterers, decorators, etc.) and their verification"
            + " status")
public class VendorController {

  private final VendorService vendorService;

  public VendorController(VendorService vendorService) {
    this.vendorService = vendorService;
  }

  @PostMapping
  @Operation(summary = "Register a new vendor")
  public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.create(request));
  }

  @GetMapping
  @Operation(
      summary =
          "List/browse vendors, paginated, optionally filtered by category and/or verification"
              + " status")
  public ResponseEntity<Page<VendorResponse>> list(
      @RequestParam(required = false) VendorCategory category,
      @RequestParam(required = false) VerificationStatus status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(vendorService.list(category, status, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a vendor by id")
  public ResponseEntity<VendorResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(vendorService.getById(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a vendor's profile")
  public ResponseEntity<VendorResponse> update(
      @PathVariable Long id, @Valid @RequestBody VendorRequest request) {
    return ResponseEntity.ok(vendorService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a vendor")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    vendorService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/verification")
  @Operation(summary = "Admin: update a vendor's verification status")
  public ResponseEntity<VendorResponse> updateVerification(
      @PathVariable Long id, @Valid @RequestBody VerificationUpdateRequest request) {
    return ResponseEntity.ok(vendorService.updateVerification(id, request));
  }

  @PatchMapping("/{id}/rating")
  @Operation(summary = "Update a vendor's aggregate rating")
  public ResponseEntity<VendorResponse> updateRating(
      @PathVariable Long id, @Valid @RequestBody RatingUpdateRequest request) {
    return ResponseEntity.ok(vendorService.updateRating(id, request));
  }
}
