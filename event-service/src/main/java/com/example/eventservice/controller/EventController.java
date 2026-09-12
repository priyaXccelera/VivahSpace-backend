package com.example.eventservice.controller;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.dto.GuestCountUpdateRequest;
import com.example.eventservice.entity.EventStatus;
import com.example.eventservice.service.EventService;
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
@RequestMapping("/api/v1/events")
@Tag(
    name = "Events",
    description = "Event workspace created upon booking confirmation of a proposal")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  @PostMapping
  @Operation(summary = "Create an event (called upon booking confirmation of a proposal)")
  public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
  }

  @GetMapping
  @Operation(summary = "List events, paginated, optionally filtered by userId")
  public ResponseEntity<Page<EventResponse>> list(
      @RequestParam(required = false) Long userId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(eventService.list(userId, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an event by id")
  public ResponseEntity<EventResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(eventService.getById(id));
  }

  @PutMapping("/{id}/guest-count")
  @Operation(summary = "Update the guest count of an event")
  public ResponseEntity<EventResponse> updateGuestCount(
      @PathVariable Long id, @Valid @RequestBody GuestCountUpdateRequest request) {
    return ResponseEntity.ok(eventService.updateGuestCount(id, request));
  }

  @PutMapping("/{id}/status")
  @Operation(summary = "Update an event's status (ACTIVE, COMPLETED, CANCELLED)")
  public ResponseEntity<EventResponse> updateStatus(
      @PathVariable Long id, @RequestParam EventStatus status) {
    return ResponseEntity.ok(eventService.updateStatus(id, status));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete an event")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    eventService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
