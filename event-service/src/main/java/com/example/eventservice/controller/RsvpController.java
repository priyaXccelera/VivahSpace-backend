package com.example.eventservice.controller;

import com.example.eventservice.dto.RsvpRequest;
import com.example.eventservice.dto.RsvpResponse;
import com.example.eventservice.service.RsvpService;
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
@RequestMapping("/api/v1/events/{eventId}/rsvps")
@Tag(name = "Event RSVPs", description = "Guest RSVP uploads for an event")
public class RsvpController {

  private final RsvpService rsvpService;

  public RsvpController(RsvpService rsvpService) {
    this.rsvpService = rsvpService;
  }

  @PostMapping
  @Operation(summary = "Add an RSVP entry for an event")
  public ResponseEntity<RsvpResponse> create(
      @PathVariable Long eventId, @Valid @RequestBody RsvpRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(rsvpService.create(eventId, request));
  }

  @GetMapping
  @Operation(summary = "List RSVP entries for an event, paginated")
  public ResponseEntity<Page<RsvpResponse>> list(
      @PathVariable Long eventId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(rsvpService.list(eventId, pageable));
  }

  @PutMapping("/{rsvpId}")
  @Operation(summary = "Update an RSVP entry")
  public ResponseEntity<RsvpResponse> update(
      @PathVariable Long eventId,
      @PathVariable Long rsvpId,
      @Valid @RequestBody RsvpRequest request) {
    return ResponseEntity.ok(rsvpService.update(eventId, rsvpId, request));
  }

  @DeleteMapping("/{rsvpId}")
  @Operation(summary = "Delete an RSVP entry")
  public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long rsvpId) {
    rsvpService.delete(eventId, rsvpId);
    return ResponseEntity.noContent().build();
  }
}
