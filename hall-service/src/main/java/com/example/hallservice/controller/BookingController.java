package com.example.hallservice.controller;

import com.example.hallservice.dto.BookingRequest;
import com.example.hallservice.dto.BookingResponse;
import com.example.hallservice.dto.WaitlistRequest;
import com.example.hallservice.dto.WaitlistResponse;
import com.example.hallservice.service.BookingService;
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
@Tag(name = "Bookings & Waitlist", description = "Book hall slots or join a waitlist")
public class BookingController {

  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @PostMapping("/api/v1/slots/{slotId}/book")
  @Operation(summary = "Book an available slot")
  public ResponseEntity<BookingResponse> bookSlot(
      @PathVariable Long slotId, @Valid @RequestBody BookingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.bookSlot(slotId, request));
  }

  @PostMapping("/api/v1/bookings/{bookingId}/cancel")
  @Operation(summary = "Cancel a booking, freeing the slot and notifying the next waitlisted user")
  public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long bookingId) {
    return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
  }

  @GetMapping("/api/v1/bookings")
  @Operation(summary = "List bookings, paginated, optionally filtered by userId")
  public ResponseEntity<Page<BookingResponse>> listBookings(
      @RequestParam(required = false) Long userId, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(bookingService.listBookings(userId, pageable));
  }

  @GetMapping("/api/v1/bookings/{id}")
  @Operation(summary = "Get a booking by id")
  public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getBooking(id));
  }

  @PostMapping("/api/v1/slots/{slotId}/waitlist")
  @Operation(summary = "Join the waitlist for a slot")
  public ResponseEntity<WaitlistResponse> joinWaitlist(
      @PathVariable Long slotId, @Valid @RequestBody WaitlistRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(bookingService.joinWaitlist(slotId, request));
  }

  @GetMapping("/api/v1/waitlist")
  @Operation(summary = "List waitlist entries, paginated, optionally filtered by hallId/slotId")
  public ResponseEntity<Page<WaitlistResponse>> listWaitlist(
      @RequestParam(required = false) Long hallId,
      @RequestParam(required = false) Long slotId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(bookingService.listWaitlist(hallId, slotId, pageable));
  }
}
