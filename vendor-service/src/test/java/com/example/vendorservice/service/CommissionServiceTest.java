package com.example.vendorservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.vendorservice.dto.CommissionRequest;
import com.example.vendorservice.dto.CommissionResponse;
import com.example.vendorservice.dto.VendorAnalyticsResponse;
import com.example.vendorservice.entity.Commission;
import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.repository.CommissionRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
class CommissionServiceTest {

  @Mock private CommissionRepository commissionRepository;

  @Mock private VendorService vendorService;

  private CommissionService commissionService;

  @BeforeEach
  void setUp() {
    commissionService = new CommissionService(commissionRepository, vendorService);
  }

  private Vendor buildVendor(Long id, BigDecimal rating) {
    Vendor vendor = new Vendor();
    vendor.setId(id);
    vendor.setBusinessName("Royal Feast Caterers");
    vendor.setRating(rating);
    return vendor;
  }

  private CommissionRequest buildRequest(long bookingId, String amount) {
    CommissionRequest request = new CommissionRequest();
    request.setBookingId(bookingId);
    request.setAmount(new BigDecimal(amount));
    return request;
  }

  private Commission buildCommission(Long id, Vendor vendor, long bookingId, String amount) {
    Commission commission = new Commission();
    commission.setId(id);
    commission.setVendor(vendor);
    commission.setBookingId(bookingId);
    commission.setAmount(new BigDecimal(amount));
    return commission;
  }

  @Test
  void record_vendorExists_savesCommissionUnderVendor() {
    Vendor vendor = buildVendor(1L, new BigDecimal("4.20"));
    when(vendorService.findVendor(1L)).thenReturn(vendor);
    Commission saved = buildCommission(100L, vendor, 1001L, "8000.00");
    when(commissionRepository.save(any(Commission.class))).thenReturn(saved);

    CommissionResponse response = commissionService.record(1L, buildRequest(1001L, "8000.00"));

    ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
    verify(commissionRepository).save(captor.capture());
    Commission passed = captor.getValue();
    assertThat(passed.getVendor()).isEqualTo(vendor);
    assertThat(passed.getBookingId()).isEqualTo(1001L);
    assertThat(passed.getAmount()).isEqualByComparingTo("8000.00");

    assertThat(response.getId()).isEqualTo(100L);
    assertThat(response.getVendorId()).isEqualTo(1L);
    assertThat(response.getBookingId()).isEqualTo(1001L);
    assertThat(response.getAmount()).isEqualByComparingTo("8000.00");
  }

  @Test
  void record_vendorMissing_throwsResourceNotFoundAndNeverSaves() {
    when(vendorService.findVendor(404L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 404"));

    assertThatThrownBy(() -> commissionService.record(404L, buildRequest(1L, "100.00")))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(commissionRepository, never()).save(any());
  }

  @Test
  void list_vendorExists_returnsMappedPage() {
    Vendor vendor = buildVendor(1L, new BigDecimal("4.20"));
    when(vendorService.findVendor(1L)).thenReturn(vendor);
    Commission commission = buildCommission(100L, vendor, 1001L, "8000.00");
    Pageable pageable = PageRequest.of(0, 20);
    when(commissionRepository.findByVendorId(eq(1L), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(commission), pageable, 1));

    Page<CommissionResponse> result = commissionService.list(1L, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getBookingId()).isEqualTo(1001L);
    verify(vendorService).findVendor(1L);
  }

  @Test
  void list_vendorMissing_throwsResourceNotFound() {
    Pageable pageable = PageRequest.of(0, 20);
    when(vendorService.findVendor(404L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 404"));

    assertThatThrownBy(() -> commissionService.list(404L, pageable))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(commissionRepository, never()).findByVendorId(any(), any());
  }

  @Test
  void analytics_vendorWithMultipleCommissions_aggregatesTotalsCorrectly() {
    Vendor vendor = buildVendor(1L, new BigDecimal("4.50"));
    when(vendorService.findVendor(1L)).thenReturn(vendor);
    Commission c1 = buildCommission(1L, vendor, 1001L, "4500.00");
    Commission c2 = buildCommission(2L, vendor, 1002L, "3200.50");
    Commission c3 = buildCommission(3L, vendor, 1003L, "1299.50");
    when(commissionRepository.findByVendorId(1L)).thenReturn(List.of(c1, c2, c3));

    VendorAnalyticsResponse response = commissionService.analytics(1L);

    assertThat(response.getVendorId()).isEqualTo(1L);
    assertThat(response.getTotalBookings()).isEqualTo(3);
    assertThat(response.getTotalCommission()).isEqualByComparingTo("9000.00");
    assertThat(response.getAverageRating()).isEqualByComparingTo("4.50");
  }

  @Test
  void analytics_vendorWithNoCommissions_returnsZeroTotalAndZeroBookings() {
    Vendor vendor = buildVendor(2L, BigDecimal.ZERO);
    when(vendorService.findVendor(2L)).thenReturn(vendor);
    when(commissionRepository.findByVendorId(2L)).thenReturn(Collections.emptyList());

    VendorAnalyticsResponse response = commissionService.analytics(2L);

    assertThat(response.getTotalBookings()).isEqualTo(0);
    assertThat(response.getTotalCommission()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void analytics_vendorMissing_throwsResourceNotFoundException() {
    when(vendorService.findVendor(999L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 999"));

    assertThatThrownBy(() -> commissionService.analytics(999L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(commissionRepository, never()).findByVendorId(any(Long.class));
  }
}
