package com.example.hallservice.controller;

import com.example.hallservice.dto.HallRequest;
import com.example.hallservice.dto.HallResponse;
import com.example.hallservice.dto.VerificationUpdateRequest;
import com.example.hallservice.service.HallService;
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
@RequestMapping("/api/v1/halls")
@Tag(name = "Halls", description = "Manage wedding halls, verification status")
public class HallController {

  private final HallService hallService;

  public HallController(HallService hallService) {
    this.hallService = hallService;
  }

  @PostMapping
  @Operation(summary = "Create a new hall")
  public ResponseEntity<HallResponse> create(@Valid @RequestBody HallRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(hallService.create(request));
  }

  @GetMapping
  @Operation(summary = "List all halls, paginated")
  public ResponseEntity<Page<HallResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(hallService.list(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a hall by id")
  public ResponseEntity<HallResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(hallService.getById(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a hall")
  public ResponseEntity<HallResponse> update(
      @PathVariable Long id, @Valid @RequestBody HallRequest request) {
    return ResponseEntity.ok(hallService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a hall")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    hallService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/verification")
  @Operation(summary = "Admin: update a hall's verification status")
  public ResponseEntity<HallResponse> updateVerification(
      @PathVariable Long id, @Valid @RequestBody VerificationUpdateRequest request) {
    return ResponseEntity.ok(hallService.updateVerification(id, request));
  }
}
