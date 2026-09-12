package com.example.vendorservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.vendorservice.dto.PortfolioItemRequest;
import com.example.vendorservice.dto.PortfolioItemResponse;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.security.JwtUtil;
import com.example.vendorservice.service.PortfolioService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(controllers = PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortfolioControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private PortfolioService portfolioService;

  @MockBean private JwtUtil jwtUtil;

  private PortfolioItemRequest buildRequest() {
    PortfolioItemRequest request = new PortfolioItemRequest();
    request.setTitle("Sharma-Reddy Wedding");
    request.setImageUrl("https://example.com/images/p1.jpg");
    request.setDescription("Candid coverage");
    return request;
  }

  @Test
  void add_validRequest_returns201() throws Exception {
    PortfolioItemResponse response = new PortfolioItemResponse();
    when(portfolioService.addItem(eq(1L), any(PortfolioItemRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/vendors/{vendorId}/portfolio", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isCreated());
  }

  @Test
  void add_vendorMissing_returns404() throws Exception {
    when(portfolioService.addItem(eq(99L), any(PortfolioItemRequest.class)))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 99"));

    mockMvc
        .perform(
            post("/api/v1/vendors/{vendorId}/portfolio", 99L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isNotFound());
  }

  @Test
  void add_blankTitle_returns400() throws Exception {
    PortfolioItemRequest request = buildRequest();
    request.setTitle("");

    mockMvc
        .perform(
            post("/api/v1/vendors/{vendorId}/portfolio", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(portfolioService, never()).addItem(any(), any());
  }

  @Test
  void list_returnsPagedItems() throws Exception {
    Page<PortfolioItemResponse> page =
        new PageImpl<>(List.of(new PortfolioItemResponse()), PageRequest.of(0, 20), 1);
    when(portfolioService.list(eq(1L), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/vendors/{vendorId}/portfolio", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void list_vendorMissing_returns404() throws Exception {
    when(portfolioService.list(eq(404L), any()))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 404"));

    mockMvc
        .perform(get("/api/v1/vendors/{vendorId}/portfolio", 404L))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_existingItem_returns204() throws Exception {
    mockMvc
        .perform(delete("/api/v1/vendors/{vendorId}/portfolio/{itemId}", 1L, 10L))
        .andExpect(status().isNoContent());

    verify(portfolioService).delete(1L, 10L);
  }

  @Test
  void delete_missingItem_returns404() throws Exception {
    doThrow(new ResourceNotFoundException("Portfolio item not found with id 55"))
        .when(portfolioService)
        .delete(1L, 55L);

    mockMvc
        .perform(delete("/api/v1/vendors/{vendorId}/portfolio/{itemId}", 1L, 55L))
        .andExpect(status().isNotFound());
  }
}
