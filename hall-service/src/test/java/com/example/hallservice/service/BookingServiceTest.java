package com.example.hallservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private WaitlistRepository waitlistRepository;
  @Mock private HallAvailabilitySlotRepository slotRepository;
  @Mock private AvailabilityService availabilityService;

  private BookingService bookingService;

  @BeforeEach
  void setUp() {
    bookingService =
        new BookingService(
            bookingRepository, waitlistRepository, slotRepository, availabilityService);
  }

  private Hall buildHall(Long id) {
    Hall hall = new Hall();
    hall.setId(id);
    hall.setName("Grand Hall");
    return hall;
  }

  private HallAvailabilitySlot buildSlot(Long id, Hall hall, SlotStatus status) {
    HallAvailabilitySlot slot = new HallAvailabilitySlot();
    slot.setId(id);
    slot.setHall(hall);
    slot.setSlotDate(LocalDate.now());
    slot.setSlotType(SlotType.MORNING);
    slot.setStatus(status);
    return slot;
  }

  private Booking buildBooking(
      Long id, Hall hall, HallAvailabilitySlot slot, BookingStatus status) {
    Booking booking = new Booking();
    booking.setId(id);
    booking.setHall(hall);
    booking.setSlot(slot);
    booking.setUserId(10L);
    booking.setStatus(status);
    return booking;
  }

  @Test
  void bookSlot_availableSlot_createsConfirmedBooking() {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.AVAILABLE);
    when(availabilityService.findSlot(2L)).thenReturn(slot);
    when(bookingRepository.save(any(Booking.class)))
        .thenAnswer(
            inv -> {
              Booking b = inv.getArgument(0);
              b.setId(100L);
              return b;
            });

    BookingRequest request = new BookingRequest();
    request.setUserId(10L);
    request.setProposalId(7L);

    BookingResponse response = bookingService.bookSlot(2L, request);

    assertThat(response.getId()).isEqualTo(100L);
    assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    assertThat(response.getHallId()).isEqualTo(1L);
    assertThat(response.getSlotId()).isEqualTo(2L);
    assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
    verify(slotRepository).save(slot);
  }

  @Test
  void bookSlot_slotAlreadyBooked_throwsSlotUnavailableException() {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    when(availabilityService.findSlot(2L)).thenReturn(slot);

    BookingRequest request = new BookingRequest();
    request.setUserId(10L);

    assertThatThrownBy(() -> bookingService.bookSlot(2L, request))
        .isInstanceOf(SlotUnavailableException.class);
    verify(bookingRepository, never()).save(any());
  }

  @Test
  void bookSlot_slotMissing_propagatesResourceNotFoundException() {
    when(availabilityService.findSlot(999L))
        .thenThrow(new ResourceNotFoundException("Slot not found with id 999"));

    BookingRequest request = new BookingRequest();
    request.setUserId(10L);

    assertThatThrownBy(() -> bookingService.bookSlot(999L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void cancelBooking_freesSlotAndNotifiesFirstWaitlistEntry() {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    Booking booking = buildBooking(50L, hall, slot, BookingStatus.CONFIRMED);
    when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));

    Waitlist waitlistEntry = new Waitlist();
    waitlistEntry.setId(1L);
    waitlistEntry.setHall(hall);
    waitlistEntry.setSlot(slot);
    waitlistEntry.setUserId(20L);
    waitlistEntry.setNotified(false);
    when(waitlistRepository.findBySlotIdAndNotifiedFalseOrderByJoinedAtAsc(2L))
        .thenReturn(List.of(waitlistEntry));

    BookingResponse response = bookingService.cancelBooking(50L);

    assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    verify(slotRepository).save(slot);

    ArgumentCaptor<Waitlist> waitlistCaptor = ArgumentCaptor.forClass(Waitlist.class);
    verify(waitlistRepository).save(waitlistCaptor.capture());
    assertThat(waitlistCaptor.getValue().isNotified()).isTrue();
  }

  @Test
  void cancelBooking_noWaitlistEntries_doesNotSaveWaitlist() {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    Booking booking = buildBooking(51L, hall, slot, BookingStatus.CONFIRMED);
    when(bookingRepository.findById(51L)).thenReturn(Optional.of(booking));
    when(waitlistRepository.findBySlotIdAndNotifiedFalseOrderByJoinedAtAsc(2L))
        .thenReturn(List.of());

    BookingResponse response = bookingService.cancelBooking(51L);

    assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    verify(waitlistRepository, never()).save(any());
  }

  @Test
  void cancelBooking_missingBooking_throwsResourceNotFoundException() {
    when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.cancelBooking(404L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void listBookings_withUserId_filtersByUser() {
    Pageable pageable = PageRequest.of(0, 20);
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    Booking booking = buildBooking(1L, hall, slot, BookingStatus.CONFIRMED);
    when(bookingRepository.findByUserId(10L, pageable))
        .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));

    Page<BookingResponse> result = bookingService.listBookings(10L, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(bookingRepository, never()).findAll(pageable);
  }

  @Test
  void listBookings_withoutUserId_returnsAll() {
    Pageable pageable = PageRequest.of(0, 20);
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    Booking booking = buildBooking(1L, hall, slot, BookingStatus.CONFIRMED);
    when(bookingRepository.findAll(pageable))
        .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));

    Page<BookingResponse> result = bookingService.listBookings(null, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(bookingRepository, never()).findByUserId(any(), any());
  }

  @Test
  void getBooking_found_returnsResponse() {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    Booking booking = buildBooking(77L, hall, slot, BookingStatus.CONFIRMED);
    when(bookingRepository.findById(77L)).thenReturn(Optional.of(booking));

    BookingResponse response = bookingService.getBooking(77L);

    assertThat(response.getId()).isEqualTo(77L);
  }

  @Test
  void getBooking_missing_throwsResourceNotFoundException() {
    when(bookingRepository.findById(1000L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.getBooking(1000L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void joinWaitlist_createsEntryNotNotified() {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall, SlotStatus.BOOKED);
    when(availabilityService.findSlot(2L)).thenReturn(slot);
    when(waitlistRepository.save(any(Waitlist.class)))
        .thenAnswer(
            inv -> {
              Waitlist w = inv.getArgument(0);
              w.setId(9L);
              return w;
            });

    WaitlistRequest request = new WaitlistRequest();
    request.setUserId(20L);

    WaitlistResponse response = bookingService.joinWaitlist(2L, request);

    assertThat(response.getId()).isEqualTo(9L);
    assertThat(response.isNotified()).isFalse();
    assertThat(response.getUserId()).isEqualTo(20L);
  }

  @Test
  void listWaitlist_bySlotId_usesSlotFilter() {
    Pageable pageable = PageRequest.of(0, 20);
    when(waitlistRepository.findBySlotId(2L, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    bookingService.listWaitlist(null, 2L, pageable);

    verify(waitlistRepository).findBySlotId(2L, pageable);
    verify(waitlistRepository, never()).findByHallId(any(), any());
    verify(waitlistRepository, never()).findAll(pageable);
  }

  @Test
  void listWaitlist_byHallId_usesHallFilter() {
    Pageable pageable = PageRequest.of(0, 20);
    when(waitlistRepository.findByHallId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    bookingService.listWaitlist(1L, null, pageable);

    verify(waitlistRepository).findByHallId(1L, pageable);
    verify(waitlistRepository, never()).findBySlotId(any(), any());
  }

  @Test
  void listWaitlist_noFilters_returnsAll() {
    Pageable pageable = PageRequest.of(0, 20);
    when(waitlistRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

    bookingService.listWaitlist(null, null, pageable);

    verify(waitlistRepository).findAll(pageable);
  }
}
