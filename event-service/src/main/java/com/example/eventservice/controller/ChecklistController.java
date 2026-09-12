package com.example.eventservice.controller;

import com.example.eventservice.dto.ChecklistItemRequest;
import com.example.eventservice.dto.ChecklistItemResponse;
import com.example.eventservice.service.ChecklistService;
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
@RequestMapping("/api/v1/events/{eventId}/checklist")
@Tag(name = "Event Checklist", description = "Event planning checklist items")
public class ChecklistController {

  private final ChecklistService checklistService;

  public ChecklistController(ChecklistService checklistService) {
    this.checklistService = checklistService;
  }

  @PostMapping
  @Operation(summary = "Add a checklist item for an event")
  public ResponseEntity<ChecklistItemResponse> create(
      @PathVariable Long eventId, @Valid @RequestBody ChecklistItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(checklistService.create(eventId, request));
  }

  @GetMapping
  @Operation(summary = "List checklist items for an event, paginated")
  public ResponseEntity<Page<ChecklistItemResponse>> list(
      @PathVariable Long eventId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(checklistService.list(eventId, pageable));
  }

  @PutMapping("/{itemId}")
  @Operation(summary = "Update a checklist item")
  public ResponseEntity<ChecklistItemResponse> update(
      @PathVariable Long eventId,
      @PathVariable Long itemId,
      @Valid @RequestBody ChecklistItemRequest request) {
    return ResponseEntity.ok(checklistService.update(eventId, itemId, request));
  }

  @DeleteMapping("/{itemId}")
  @Operation(summary = "Delete a checklist item")
  public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long itemId) {
    checklistService.delete(eventId, itemId);
    return ResponseEntity.noContent().build();
  }
}
