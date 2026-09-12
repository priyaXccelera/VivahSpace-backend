package com.example.proposalservice.controller;

import com.example.proposalservice.dto.RevenueAnalyticsResponse;
import com.example.proposalservice.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(
    name = "Revenue Analytics",
    description = "Admin revenue-split analytics across confirmed bookings")
public class AnalyticsController {

  private final ProposalService proposalService;

  public AnalyticsController(ProposalService proposalService) {
    this.proposalService = proposalService;
  }

  @GetMapping("/revenue")
  @Operation(
      summary = "Total revenue, hall revenue and vendor revenue split across confirmed bookings")
  public ResponseEntity<RevenueAnalyticsResponse> revenue() {
    return ResponseEntity.ok(proposalService.revenue());
  }
}
