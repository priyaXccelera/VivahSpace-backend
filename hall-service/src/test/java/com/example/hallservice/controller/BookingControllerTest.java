package com.example.hallservice.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.hallservice.dto.BookingRequest;
import com.example.hallservice.dto.BookingResponse;
import com.example.hallservice.dto.WaitlistRequest;
import com.example.hallservice.dto.WaitlistResponse;
import com.example.hallservice.entity.*;
import com.example.hallservice.exception.GlobalExceptionHandler;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.exception.SlotUnavailableException;
import com.example.hallservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

  @Mock private BookingService bookingService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    BookingController controller = new BookingController(bookingService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  private Hall buildHall(Long id) {
    Hall hall = new Hall();
    hall.setId(id);
    hall.setName("Grand Hall");
    return hall;
  }

  private HallAvailabilitySlot buildSlot(Long id, Hall hall) {
    HallAvailabilitySlot slot = new HallAvailabilitySlot();
    slot.setId(id);
    slot.setHall(hall);
    slot.setSlotDate(LocalDate.now());
    slot.setSlotType(SlotType.MORNING);
    slot.setStatus(SlotStatus.AVAILABLE);
    return slot;
  }

  private BookingResponse buildBookingResponse(Long id) {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall);
    Booking booking = new Booking();
    booking.setId(id);
    booking.setHall(hall);
    booking.setSlot(slot);
    booking.setUserId(10L);
    booking.setStatus(BookingStatus.CONFIRMED);
    return BookingResponse.from(booking);
  }

  private WaitlistResponse buildWaitlistResponse(Long id) {
    Hall hall = buildHall(1L);
    HallAvailabilitySlot slot = buildSlot(2L, hall);
    Waitlist waitlist = new Waitlist();
    waitlist.setId(id);
    waitlist.setHall(hall);
    waitlist.setSlot(slot);
    waitlist.setUserId(20L);
    waitlist.setNotified(false);
    return WaitlistResponse.from(waitlist);
  }

  @Test
  void bookSlot_available_returns201() throws Exception {
    when(bookingService.bookSlot(eq(2L), any(BookingRequest.class)))
        .thenReturn(buildBookingResponse(100L));

    BookingRequest request = new BookingRequest();
    request.setUserId(10L);

    mockMvc
        .perform(
            post("/api/v1/slots/2/book")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void bookSlot_unavailable_returns409() throws Exception {
    when(bookingService.bookSlot(eq(2L), any(BookingRequest.class)))
        .thenThrow(
            new SlotUnavailableException(
                "Slot 2 is not available for booking. Join the waitlist instead."));

    BookingRequest request = new BookingRequest();
    request.setUserId(10L);

    mockMvc
        .perform(
            post("/api/v1/slots/2/book")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void bookSlot_missingUserId_returns400() throws Exception {
    BookingRequest request = new BookingRequest();

    mockMvc
        .perform(
            post("/api/v1/slots/2/book")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cancelBooking_returnsCancelledBooking() throws Exception {
    BookingResponse cancelled = buildBookingResponse(50L);
    when(bookingService.cancelBooking(50L)).thenReturn(cancelled);

    mockMvc
        .perform(post("/api/v1/bookings/50/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(50));
  }

  @Test
  void cancelBooking_missing_returns404() throws Exception {
    when(bookingService.cancelBooking(404L))
        .thenThrow(new ResourceNotFoundException("Booking not found with id 404"));

    mockMvc.perform(post("/api/v1/bookings/404/cancel")).andExpect(status().isNotFound());
  }

  @Test
  void listBookings_returnsPage() throws Exception {
    Pageable pageable = PageRequest.of(0, 20);
    when(bookingService.listBookings(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(buildBookingResponse(1L)), pageable, 1));

    mockMvc
        .perform(get("/api/v1/bookings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1));
  }

  @Test
  void getBooking_found_returns200() throws Exception {
    when(bookingService.getBooking(1L)).thenReturn(buildBookingResponse(1L));

    mockMvc
        .perform(get("/api/v1/bookings/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void joinWaitlist_returns201() throws Exception {
    when(bookingService.joinWaitlist(eq(2L), any(WaitlistRequest.class)))
        .thenReturn(buildWaitlistResponse(9L));

    WaitlistRequest request = new WaitlistRequest();
    request.setUserId(20L);

    mockMvc
        .perform(
            post("/api/v1/slots/2/waitlist")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.notified").value(false));
  }

  @Test
  void listWaitlist_returnsPage() throws Exception {
    Pageable pageable = PageRequest.of(0, 20);
    when(bookingService.listWaitlist(any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(buildWaitlistResponse(1L)), pageable, 1));

    mockMvc
        .perform(get("/api/v1/waitlist"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1));
  }
}
