package com.example.hallservice.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.hallservice.dto.AvailabilityQueryResponse;
import com.example.hallservice.dto.SlotRequest;
import com.example.hallservice.dto.SlotResponse;
import com.example.hallservice.entity.Hall;
import com.example.hallservice.entity.HallAvailabilitySlot;
import com.example.hallservice.entity.SlotStatus;
import com.example.hallservice.entity.SlotType;
import com.example.hallservice.exception.GlobalExceptionHandler;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.exception.SlotUnavailableException;
import com.example.hallservice.service.AvailabilityService;
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
class AvailabilityControllerTest {

  @Mock private AvailabilityService availabilityService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    AvailabilityController controller = new AvailabilityController(availabilityService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  private SlotResponse buildSlotResponse(Long id) {
    Hall hall = new Hall();
    hall.setId(1L);
    hall.setName("Grand Hall");
    HallAvailabilitySlot slot = new HallAvailabilitySlot();
    slot.setId(id);
    slot.setHall(hall);
    slot.setSlotDate(LocalDate.of(2024, 12, 25));
    slot.setSlotType(SlotType.MORNING);
    slot.setStatus(SlotStatus.AVAILABLE);
    return SlotResponse.from(slot);
  }

  @Test
  void createSlot_valid_returns201() throws Exception {
    when(availabilityService.createSlot(eq(1L), any(SlotRequest.class)))
        .thenReturn(buildSlotResponse(5L));

    SlotRequest request = new SlotRequest();
    request.setSlotDate(LocalDate.of(2024, 12, 25));
    request.setSlotType(SlotType.MORNING);

    mockMvc
        .perform(
            post("/api/v1/halls/1/slots")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void createSlot_duplicate_returns409() throws Exception {
    when(availabilityService.createSlot(eq(1L), any(SlotRequest.class)))
        .thenThrow(
            new SlotUnavailableException(
                "A slot of type MORNING already exists for this hall on 2024-12-25"));

    SlotRequest request = new SlotRequest();
    request.setSlotDate(LocalDate.of(2024, 12, 25));
    request.setSlotType(SlotType.MORNING);

    mockMvc
        .perform(
            post("/api/v1/halls/1/slots")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void createSlot_missingHall_returns404() throws Exception {
    when(availabilityService.createSlot(eq(1L), any(SlotRequest.class)))
        .thenThrow(new ResourceNotFoundException("Hall not found with id 1"));

    SlotRequest request = new SlotRequest();
    request.setSlotDate(LocalDate.of(2024, 12, 25));
    request.setSlotType(SlotType.MORNING);

    mockMvc
        .perform(
            post("/api/v1/halls/1/slots")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void createSlot_invalidBody_returns400() throws Exception {
    SlotRequest request = new SlotRequest();

    mockMvc
        .perform(
            post("/api/v1/halls/1/slots")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listSlots_returnsPage() throws Exception {
    Pageable pageable = PageRequest.of(0, 20);
    when(availabilityService.listSlots(eq(1L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(buildSlotResponse(1L)), pageable, 1));

    mockMvc
        .perform(get("/api/v1/halls/1/slots"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1));
  }

  @Test
  void queryAvailability_returnsAvailableSlots() throws Exception {
    AvailabilityQueryResponse response =
        new AvailabilityQueryResponse(1L, List.of(buildSlotResponse(1L)), List.of());
    when(availabilityService.queryAvailability(eq(1L), eq(LocalDate.of(2024, 12, 25))))
        .thenReturn(response);

    mockMvc
        .perform(get("/api/v1/halls/1/slots/available").param("date", "2024-12-25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hallId").value(1))
        .andExpect(jsonPath("$.availableSlots[0].id").value(1))
        .andExpect(jsonPath("$.suggestedAlternatives").isEmpty());
  }
}
