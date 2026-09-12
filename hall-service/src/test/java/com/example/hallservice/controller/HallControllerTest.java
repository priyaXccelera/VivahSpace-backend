package com.example.hallservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.hallservice.dto.HallRequest;
import com.example.hallservice.dto.HallResponse;
import com.example.hallservice.dto.VerificationUpdateRequest;
import com.example.hallservice.entity.Hall;
import com.example.hallservice.entity.VerificationStatus;
import com.example.hallservice.exception.GlobalExceptionHandler;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.service.HallService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
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
class HallControllerTest {

  @Mock private HallService hallService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    HallController controller = new HallController(hallService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  private HallResponse buildResponse(Long id) {
    Hall hall = new Hall();
    hall.setId(id);
    hall.setOwnerName("Jane Owner");
    hall.setName("Grand Hall");
    hall.setLocation("Downtown");
    hall.setDescription("desc");
    hall.setCapacity(200);
    hall.setBasePrice(new BigDecimal("1500.00"));
    hall.setVerificationStatus(VerificationStatus.UNVERIFIED);
    return HallResponse.from(hall);
  }

  private String validHallRequestJson() throws Exception {
    HallRequest request = new HallRequest();
    request.setOwnerName("Jane Owner");
    request.setName("Grand Hall");
    request.setLocation("Downtown");
    request.setDescription("desc");
    request.setCapacity(200);
    request.setBasePrice(new BigDecimal("1500.00"));
    return objectMapper.writeValueAsString(request);
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    when(hallService.create(any(HallRequest.class))).thenReturn(buildResponse(1L));

    mockMvc
        .perform(
            post("/api/v1/halls").contentType("application/json").content(validHallRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Grand Hall"));
  }

  @Test
  void create_invalidRequest_returns400() throws Exception {
    HallRequest invalid = new HallRequest();
    invalid.setName("");

    mockMvc
        .perform(
            post("/api/v1/halls")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());

    verify(hallService, never()).create(any());
  }

  @Test
  void list_returnsPageOfHalls() throws Exception {
    Pageable pageable = PageRequest.of(0, 20);
    when(hallService.list(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(buildResponse(1L)), pageable, 1));

    mockMvc
        .perform(get("/api/v1/halls"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1));
  }

  @Test
  void getById_found_returns200() throws Exception {
    when(hallService.getById(1L)).thenReturn(buildResponse(1L));

    mockMvc
        .perform(get("/api/v1/halls/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    when(hallService.getById(99L))
        .thenThrow(new ResourceNotFoundException("Hall not found with id 99"));

    mockMvc
        .perform(get("/api/v1/halls/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Hall not found with id 99"));
  }

  @Test
  void update_validRequest_returns200() throws Exception {
    when(hallService.update(eq(1L), any(HallRequest.class))).thenReturn(buildResponse(1L));

    mockMvc
        .perform(
            put("/api/v1/halls/1").contentType("application/json").content(validHallRequestJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void delete_existingHall_returns204() throws Exception {
    doNothing().when(hallService).delete(1L);

    mockMvc.perform(delete("/api/v1/halls/1")).andExpect(status().isNoContent());

    verify(hallService).delete(1L);
  }

  @Test
  void updateVerification_returnsUpdatedHall() throws Exception {
    HallResponse verified = buildResponse(1L);
    when(hallService.updateVerification(eq(1L), any(VerificationUpdateRequest.class)))
        .thenReturn(verified);

    VerificationUpdateRequest request = new VerificationUpdateRequest();
    request.setStatus(VerificationStatus.VERIFIED);

    mockMvc
        .perform(
            patch("/api/v1/halls/1/verification")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}
