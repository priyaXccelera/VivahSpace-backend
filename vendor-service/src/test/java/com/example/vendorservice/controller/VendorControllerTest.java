package com.example.vendorservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.vendorservice.dto.RatingUpdateRequest;
import com.example.vendorservice.dto.VendorRequest;
import com.example.vendorservice.dto.VendorResponse;
import com.example.vendorservice.dto.VerificationUpdateRequest;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.security.JwtUtil;
import com.example.vendorservice.service.VendorService;
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

@WebMvcTest(controllers = VendorController.class)
@AutoConfigureMockMvc(addFilters = false)
class VendorControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private VendorService vendorService;

  @MockBean private JwtUtil jwtUtil;

  private VendorRequest buildRequest() {
    VendorRequest request = new VendorRequest();
    request.setOwnerName("Anita Sharma");
    request.setBusinessName("Anita Photography");
    request.setCategory(VendorCategory.PHOTOGRAPHY);
    request.setLocation("Mumbai");
    request.setDescription("Candid wedding photography");
    request.setBasePrice(new BigDecimal("45000.00"));
    request.setContactEmail("anita@example.com");
    request.setContactPhone("9876500001");
    return request;
  }

  private VendorResponse buildResponse(Long id) {
    VendorRequest request = buildRequest();
    com.example.vendorservice.entity.Vendor vendor = new com.example.vendorservice.entity.Vendor();
    vendor.setId(id);
    vendor.setOwnerName(request.getOwnerName());
    vendor.setBusinessName(request.getBusinessName());
    vendor.setCategory(request.getCategory());
    vendor.setLocation(request.getLocation());
    vendor.setDescription(request.getDescription());
    vendor.setBasePrice(request.getBasePrice());
    vendor.setContactEmail(request.getContactEmail());
    vendor.setContactPhone(request.getContactPhone());
    vendor.setVerificationStatus(VerificationStatus.UNVERIFIED);
    vendor.setRating(BigDecimal.ZERO);
    return VendorResponse.from(vendor);
  }

  @Test
  void create_validRequest_returns201WithBody() throws Exception {
    VendorResponse response = buildResponse(1L);
    when(vendorService.create(any(VendorRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/vendors")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.businessName").value("Anita Photography"));
  }

  @Test
  void create_invalidRequest_returns400() throws Exception {
    VendorRequest invalid = buildRequest();
    invalid.setOwnerName("");
    invalid.setContactEmail("not-an-email");

    mockMvc
        .perform(
            post("/api/v1/vendors")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());

    verify(vendorService, never()).create(any());
  }

  @Test
  void getById_found_returns200() throws Exception {
    when(vendorService.getById(1L)).thenReturn(buildResponse(1L));

    mockMvc
        .perform(get("/api/v1/vendors/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.ownerName").value("Anita Sharma"));
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    when(vendorService.getById(99L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 99"));

    mockMvc
        .perform(get("/api/v1/vendors/{id}", 99L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Vendor not found with id 99"));
  }

  @Test
  void list_returnsPagedVendors() throws Exception {
    Page<VendorResponse> page =
        new PageImpl<>(List.of(buildResponse(1L)), PageRequest.of(0, 20), 1);
    when(vendorService.list(eq(null), eq(null), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/vendors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void list_withCategoryFilter_passesCategoryToService() throws Exception {
    Page<VendorResponse> page = new PageImpl<>(List.of(buildResponse(1L)));
    when(vendorService.list(eq(VendorCategory.PHOTOGRAPHY), eq(null), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/vendors").param("category", "PHOTOGRAPHY"))
        .andExpect(status().isOk());

    verify(vendorService).list(eq(VendorCategory.PHOTOGRAPHY), eq(null), any());
  }

  @Test
  void update_validRequest_returns200() throws Exception {
    VendorResponse updated = buildResponse(1L);
    when(vendorService.update(eq(1L), any(VendorRequest.class))).thenReturn(updated);

    mockMvc
        .perform(
            put("/api/v1/vendors/{id}", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void update_vendorMissing_returns404() throws Exception {
    when(vendorService.update(eq(42L), any(VendorRequest.class)))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 42"));

    mockMvc
        .perform(
            put("/api/v1/vendors/{id}", 42L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_existingVendor_returns204() throws Exception {
    mockMvc.perform(delete("/api/v1/vendors/{id}", 1L)).andExpect(status().isNoContent());

    verify(vendorService).delete(1L);
  }

  @Test
  void delete_missingVendor_returns404() throws Exception {
    doThrow(new ResourceNotFoundException("Vendor not found with id 5"))
        .when(vendorService)
        .delete(5L);

    mockMvc.perform(delete("/api/v1/vendors/{id}", 5L)).andExpect(status().isNotFound());
  }

  @Test
  void updateVerification_validRequest_returns200() throws Exception {
    VendorResponse response = buildResponse(1L);
    when(vendorService.updateVerification(eq(1L), any(VerificationUpdateRequest.class)))
        .thenReturn(response);

    VerificationUpdateRequest request = new VerificationUpdateRequest();
    request.setStatus(VerificationStatus.VERIFIED);

    mockMvc
        .perform(
            patch("/api/v1/vendors/{id}/verification", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void updateVerification_missingStatus_returns400() throws Exception {
    VerificationUpdateRequest request = new VerificationUpdateRequest();

    mockMvc
        .perform(
            patch("/api/v1/vendors/{id}/verification", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRating_validRequest_returns200() throws Exception {
    VendorResponse response = buildResponse(1L);
    when(vendorService.updateRating(eq(1L), any(RatingUpdateRequest.class))).thenReturn(response);

    RatingUpdateRequest request = new RatingUpdateRequest();
    request.setRating(new BigDecimal("4.5"));

    mockMvc
        .perform(
            patch("/api/v1/vendors/{id}/rating", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void updateRating_outOfRange_returns400() throws Exception {
    RatingUpdateRequest request = new RatingUpdateRequest();
    request.setRating(new BigDecimal("9.9"));

    mockMvc
        .perform(
            patch("/api/v1/vendors/{id}/rating", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(vendorService, never()).updateRating(any(), any());
  }
}
