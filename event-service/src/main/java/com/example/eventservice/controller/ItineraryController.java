package com.example.eventservice.controller;

import com.example.eventservice.dto.ItineraryEntryRequest;
import com.example.eventservice.dto.ItineraryEntryResponse;
import com.example.eventservice.service.ItineraryService;
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
@RequestMapping("/api/v1/events/{eventId}/itinerary")
@Tag(name = "Event Itinerary", description = "Scheduled event-day activities")
public class ItineraryController {

  private final ItineraryService itineraryService;

  public ItineraryController(ItineraryService itineraryService) {
    this.itineraryService = itineraryService;
  }

  @PostMapping
  @Operation(summary = "Add an itinerary entry for an event")
  public ResponseEntity<ItineraryEntryResponse> create(
      @PathVariable Long eventId, @Valid @RequestBody ItineraryEntryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(itineraryService.create(eventId, request));
  }

  @GetMapping
  @Operation(summary = "List itinerary entries for an event, paginated")
  public ResponseEntity<Page<ItineraryEntryResponse>> list(
      @PathVariable Long eventId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(itineraryService.list(eventId, pageable));
  }

  @PutMapping("/{entryId}")
  @Operation(summary = "Update an itinerary entry")
  public ResponseEntity<ItineraryEntryResponse> update(
      @PathVariable Long eventId,
      @PathVariable Long entryId,
      @Valid @RequestBody ItineraryEntryRequest request) {
    return ResponseEntity.ok(itineraryService.update(eventId, entryId, request));
  }

  @DeleteMapping("/{entryId}")
  @Operation(summary = "Delete an itinerary entry")
  public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long entryId) {
    itineraryService.delete(eventId, entryId);
    return ResponseEntity.noContent().build();
  }
}
