package com.example.hallservice.service;

import com.example.hallservice.dto.BookingRequest;
import com.example.hallservice.dto.BookingResponse;
import com.example.hallservice.dto.WaitlistRequest;
import com.example.hallservice.dto.WaitlistResponse;
import com.example.hallservice.entity.*;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.exception.SlotUnavailableException;
import com.example.hallservice.repository.BookingRepository;
import com.example.hallservice.repository.HallAvailabilitySlotRepository;
import com.example.hallservice.repository.WaitlistRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

  private final BookingRepository bookingRepository;
  private final WaitlistRepository waitlistRepository;
  private final HallAvailabilitySlotRepository slotRepository;
  private final AvailabilityService availabilityService;

  public BookingService(
      BookingRepository bookingRepository,
      WaitlistRepository waitlistRepository,
      HallAvailabilitySlotRepository slotRepository,
      AvailabilityService availabilityService) {
    this.bookingRepository = bookingRepository;
    this.waitlistRepository = waitlistRepository;
    this.slotRepository = slotRepository;
    this.availabilityService = availabilityService;
  }

  @Transactional
  public BookingResponse bookSlot(Long slotId, BookingRequest request) {
    HallAvailabilitySlot slot = availabilityService.findSlot(slotId);
    if (slot.getStatus() != SlotStatus.AVAILABLE) {
      throw new SlotUnavailableException(
          "Slot " + slotId + " is not available for booking. Join the waitlist instead.");
    }
    Booking booking = new Booking();
    booking.setHall(slot.getHall());
    booking.setSlot(slot);
    booking.setUserId(request.getUserId());
    booking.setProposalId(request.getProposalId());
    booking.setStatus(BookingStatus.CONFIRMED);
    Booking saved = bookingRepository.save(booking);

    slot.setStatus(SlotStatus.BOOKED);
    slotRepository.save(slot);

    return BookingResponse.from(saved);
  }

  @Transactional
  public BookingResponse cancelBooking(Long bookingId) {
    Booking booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id " + bookingId));
    booking.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(booking);

    HallAvailabilitySlot slot = booking.getSlot();
    slot.setStatus(SlotStatus.AVAILABLE);
    slotRepository.save(slot);

    // notify the first waiting user in the waitlist (waitlist notification simulation)
    List<Waitlist> waiting =
        waitlistRepository.findBySlotIdAndNotifiedFalseOrderByJoinedAtAsc(slot.getId());
    if (!waiting.isEmpty()) {
      Waitlist first = waiting.get(0);
      first.setNotified(true);
      waitlistRepository.save(first);
    }

    return BookingResponse.from(booking);
  }

  @Transactional(readOnly = true)
  public Page<BookingResponse> listBookings(Long userId, Pageable pageable) {
    Page<Booking> page =
        userId != null
            ? bookingRepository.findByUserId(userId, pageable)
            : bookingRepository.findAll(pageable);
    return page.map(BookingResponse::from);
  }

  @Transactional(readOnly = true)
  public BookingResponse getBooking(Long id) {
    return bookingRepository
        .findById(id)
        .map(BookingResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
  }

  @Transactional
  public WaitlistResponse joinWaitlist(Long slotId, WaitlistRequest request) {
    HallAvailabilitySlot slot = availabilityService.findSlot(slotId);
    Waitlist waitlist = new Waitlist();
    waitlist.setHall(slot.getHall());
    waitlist.setSlot(slot);
    waitlist.setUserId(request.getUserId());
    waitlist.setNotified(false);
    return WaitlistResponse.from(waitlistRepository.save(waitlist));
  }

  @Transactional(readOnly = true)
  public Page<WaitlistResponse> listWaitlist(Long hallId, Long slotId, Pageable pageable) {
    Page<Waitlist> page;
    if (slotId != null) {
      page = waitlistRepository.findBySlotId(slotId, pageable);
    } else if (hallId != null) {
      page = waitlistRepository.findByHallId(hallId, pageable);
    } else {
      page = waitlistRepository.findAll(pageable);
    }
    return page.map(WaitlistResponse::from);
  }
}
