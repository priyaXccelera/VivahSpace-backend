package com.example.hallservice.controller;

import com.example.hallservice.dto.AvailabilityQueryResponse;
import com.example.hallservice.dto.SlotRequest;
import com.example.hallservice.dto.SlotResponse;
import com.example.hallservice.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/halls/{hallId}/slots")
@Tag(name = "Hall Availability", description = "Smart availability management for hall slots")
public class AvailabilityController {

  private final AvailabilityService availabilityService;

  public AvailabilityController(AvailabilityService availabilityService) {
    this.availabilityService = availabilityService;
  }

  @PostMapping
  @Operation(summary = "Create an availability slot for a hall")
  public ResponseEntity<SlotResponse> createSlot(
      @PathVariable Long hallId, @Valid @RequestBody SlotRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(availabilityService.createSlot(hallId, request));
  }

  @GetMapping
  @Operation(summary = "List all slots for a hall, paginated")
  public ResponseEntity<Page<SlotResponse>> listSlots(
      @PathVariable Long hallId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(availabilityService.listSlots(hallId, pageable));
  }

  @GetMapping("/available")
  @Operation(
      summary =
          "Query available slots for a hall/date; auto-suggests alternative halls when unavailable")
  public ResponseEntity<AvailabilityQueryResponse> queryAvailability(
      @PathVariable Long hallId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(availabilityService.queryAvailability(hallId, date));
  }
}
