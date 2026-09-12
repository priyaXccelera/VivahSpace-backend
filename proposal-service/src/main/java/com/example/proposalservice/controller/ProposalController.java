package com.example.proposalservice.controller;

import com.example.proposalservice.dto.*;
import com.example.proposalservice.service.ProposalService;
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
@RequestMapping("/api/v1/proposals")
@Tag(
    name = "Proposals",
    description =
        "Build custom quotes combining a hall and vendor services, with a live estimated total")
public class ProposalController {

  private final ProposalService proposalService;

  public ProposalController(ProposalService proposalService) {
    this.proposalService = proposalService;
  }

  @PostMapping
  @Operation(summary = "Create a draft proposal for a hall")
  public ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.create(request));
  }

  @GetMapping
  @Operation(summary = "List proposals, paginated, optionally filtered by userId")
  public ResponseEntity<Page<ProposalResponse>> list(
      @RequestParam(required = false) Long userId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(proposalService.list(userId, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a proposal by id")
  public ResponseEntity<ProposalResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(proposalService.getById(id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a proposal")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    proposalService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/items")
  @Operation(
      summary = "Add a vendor service item to a proposal; recomputes the live estimated total")
  public ResponseEntity<ProposalResponse> addItem(
      @PathVariable Long id, @Valid @RequestBody ProposalItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.addItem(id, request));
  }

  @DeleteMapping("/{id}/items/{itemId}")
  @Operation(
      summary = "Remove a vendor service item from a proposal; recomputes the live estimated total")
  public ResponseEntity<ProposalResponse> removeItem(
      @PathVariable Long id, @PathVariable Long itemId) {
    return ResponseEntity.ok(proposalService.removeItem(id, itemId));
  }

  @PostMapping("/{id}/coupon")
  @Operation(summary = "Apply a coupon code to a proposal")
  public ResponseEntity<ProposalResponse> applyCoupon(
      @PathVariable Long id, @Valid @RequestBody CouponApplyRequest request) {
    return ResponseEntity.ok(proposalService.applyCoupon(id, request));
  }

  @PostMapping("/{id}/share")
  @Operation(summary = "Mark a proposal as shared")
  public ResponseEntity<ProposalResponse> share(@PathVariable Long id) {
    return ResponseEntity.ok(proposalService.share(id));
  }

  @PostMapping("/{id}/book")
  @Operation(
      summary = "Book a proposal: reserves the hall slot and creates the resulting event workspace")
  public ResponseEntity<BookProposalResponse> book(
      @PathVariable Long id, @Valid @RequestBody BookProposalRequest request) {
    return ResponseEntity.ok(proposalService.book(id, request));
  }
}
