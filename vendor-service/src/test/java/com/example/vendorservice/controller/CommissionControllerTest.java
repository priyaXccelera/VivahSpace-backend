package com.example.vendorservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.vendorservice.dto.CommissionRequest;
import com.example.vendorservice.dto.CommissionResponse;
import com.example.vendorservice.dto.VendorAnalyticsResponse;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.security.JwtUtil;
import com.example.vendorservice.service.CommissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommissionControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private CommissionService commissionService;

  @MockBean private JwtUtil jwtUtil;

  private CommissionRequest buildRequest() {
    CommissionRequest request = new CommissionRequest();
    request.setBookingId(1001L);
    request.setAmount(new BigDecimal("8000.00"));
    return request;
  }

  @Test
  void record_validRequest_returns201() throws Exception {
    CommissionResponse response = new CommissionResponse();
    when(commissionService.record(eq(1L), any(CommissionRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/vendors/{vendorId}/commissions", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isCreated());
  }

  @Test
  void record_vendorMissing_returns404() throws Exception {
    when(commissionService.record(eq(99L), any(CommissionRequest.class)))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 99"));

    mockMvc
        .perform(
            post("/api/v1/vendors/{vendorId}/commissions", 99L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isNotFound());
  }

  @Test
  void record_invalidAmount_returns400() throws Exception {
    CommissionRequest request = buildRequest();
    request.setAmount(new BigDecimal("-5.00"));

    mockMvc
        .perform(
            post("/api/v1/vendors/{vendorId}/commissions", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(commissionService, never()).record(any(), any());
  }

  @Test
  void list_returnsPagedCommissions() throws Exception {
    Page<CommissionResponse> page =
        new PageImpl<>(List.of(new CommissionResponse()), PageRequest.of(0, 20), 1);
    when(commissionService.list(eq(1L), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/vendors/{vendorId}/commissions", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void list_vendorMissing_returns404() throws Exception {
    when(commissionService.list(eq(404L), any()))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 404"));

    mockMvc
        .perform(get("/api/v1/vendors/{vendorId}/commissions", 404L))
        .andExpect(status().isNotFound());
  }

  @Test
  void analytics_returnsAnalyticsPayload() throws Exception {
    VendorAnalyticsResponse response =
        new VendorAnalyticsResponse(1L, 3L, new BigDecimal("9000.00"), new BigDecimal("4.50"));
    when(commissionService.analytics(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/vendors/{vendorId}/analytics", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vendorId").value(1))
        .andExpect(jsonPath("$.totalBookings").value(3))
        .andExpect(jsonPath("$.totalCommission").value(9000.00))
        .andExpect(jsonPath("$.averageRating").value(4.50));
  }

  @Test
  void analytics_vendorMissing_returns404() throws Exception {
    when(commissionService.analytics(500L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 500"));

    mockMvc
        .perform(get("/api/v1/vendors/{vendorId}/analytics", 500L))
        .andExpect(status().isNotFound());
  }
}
