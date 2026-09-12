package com.example.vendorservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.vendorservice.dto.RatingUpdateRequest;
import com.example.vendorservice.dto.VendorRequest;
import com.example.vendorservice.dto.VendorResponse;
import com.example.vendorservice.dto.VerificationUpdateRequest;
import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.repository.VendorRepository;
import java.math.BigDecimal;
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
class VendorServiceTest {

  @Mock private VendorRepository vendorRepository;

  private VendorService vendorService;

  @BeforeEach
  void setUp() {
    vendorService = new VendorService(vendorRepository);
  }

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

  private Vendor buildVendor(Long id) {
    Vendor vendor = new Vendor();
    vendor.setId(id);
    vendor.setOwnerName("Anita Sharma");
    vendor.setBusinessName("Anita Photography");
    vendor.setCategory(VendorCategory.PHOTOGRAPHY);
    vendor.setLocation("Mumbai");
    vendor.setDescription("Candid wedding photography");
    vendor.setBasePrice(new BigDecimal("45000.00"));
    vendor.setContactEmail("anita@example.com");
    vendor.setContactPhone("9876500001");
    vendor.setVerificationStatus(VerificationStatus.UNVERIFIED);
    vendor.setRating(BigDecimal.ZERO);
    return vendor;
  }

  @Test
  void create_savesVendorAndReturnsResponse() {
    VendorRequest request = buildRequest();
    Vendor saved = buildVendor(1L);
    when(vendorRepository.save(any(Vendor.class))).thenReturn(saved);

    VendorResponse response = vendorService.create(request);

    ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
    verify(vendorRepository).save(captor.capture());
    Vendor passed = captor.getValue();
    assertThat(passed.getOwnerName()).isEqualTo("Anita Sharma");
    assertThat(passed.getBusinessName()).isEqualTo("Anita Photography");
    assertThat(passed.getCategory()).isEqualTo(VendorCategory.PHOTOGRAPHY);
    assertThat(passed.getBasePrice()).isEqualTo(new BigDecimal("45000.00"));

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getBusinessName()).isEqualTo("Anita Photography");
    assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
  }

  @Test
  void list_noFilters_delegatesToFindAll() {
    Vendor vendor = buildVendor(1L);
    Pageable pageable = PageRequest.of(0, 20);
    when(vendorRepository.findAll(pageable))
        .thenReturn(new PageImpl<>(List.of(vendor), pageable, 1));

    Page<VendorResponse> result = vendorService.list(null, null, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getBusinessName()).isEqualTo("Anita Photography");
    verify(vendorRepository).findAll(pageable);
    verify(vendorRepository, never()).findByCategory(any(), any());
    verify(vendorRepository, never()).findByVerificationStatus(any(), any());
    verify(vendorRepository, never()).findByCategoryAndVerificationStatus(any(), any(), any());
  }

  @Test
  void list_categoryOnly_delegatesToFindByCategory() {
    Vendor vendor = buildVendor(1L);
    Pageable pageable = PageRequest.of(0, 20);
    when(vendorRepository.findByCategory(eq(VendorCategory.PHOTOGRAPHY), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(vendor), pageable, 1));

    Page<VendorResponse> result = vendorService.list(VendorCategory.PHOTOGRAPHY, null, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(vendorRepository).findByCategory(VendorCategory.PHOTOGRAPHY, pageable);
    verify(vendorRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void list_statusOnly_delegatesToFindByVerificationStatus() {
    Vendor vendor = buildVendor(1L);
    Pageable pageable = PageRequest.of(0, 20);
    when(vendorRepository.findByVerificationStatus(eq(VerificationStatus.VERIFIED), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(vendor), pageable, 1));

    Page<VendorResponse> result = vendorService.list(null, VerificationStatus.VERIFIED, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(vendorRepository).findByVerificationStatus(VerificationStatus.VERIFIED, pageable);
  }

  @Test
  void list_categoryAndStatus_delegatesToCombinedFinder() {
    Vendor vendor = buildVendor(1L);
    Pageable pageable = PageRequest.of(0, 20);
    when(vendorRepository.findByCategoryAndVerificationStatus(
            eq(VendorCategory.PHOTOGRAPHY), eq(VerificationStatus.VERIFIED), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(vendor), pageable, 1));

    Page<VendorResponse> result =
        vendorService.list(VendorCategory.PHOTOGRAPHY, VerificationStatus.VERIFIED, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(vendorRepository)
        .findByCategoryAndVerificationStatus(
            VendorCategory.PHOTOGRAPHY, VerificationStatus.VERIFIED, pageable);
  }

  @Test
  void getById_found_returnsResponse() {
    Vendor vendor = buildVendor(1L);
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

    VendorResponse response = vendorService.getById(1L);

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getBusinessName()).isEqualTo("Anita Photography");
  }

  @Test
  void getById_notFound_throwsResourceNotFoundException() {
    when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vendorService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void update_found_appliesChangesAndSaves() {
    Vendor existing = buildVendor(1L);
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

    VendorRequest request = buildRequest();
    request.setBusinessName("Updated Photography Studio");
    request.setBasePrice(new BigDecimal("50000.00"));

    VendorResponse response = vendorService.update(1L, request);

    assertThat(response.getBusinessName()).isEqualTo("Updated Photography Studio");
    assertThat(response.getBasePrice()).isEqualTo(new BigDecimal("50000.00"));
    verify(vendorRepository).save(existing);
  }

  @Test
  void update_notFound_throwsResourceNotFoundException() {
    when(vendorRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vendorService.update(42L, buildRequest()))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(vendorRepository, never()).save(any());
  }

  @Test
  void delete_found_deletesVendor() {
    Vendor existing = buildVendor(1L);
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(existing));

    vendorService.delete(1L);

    verify(vendorRepository).delete(existing);
  }

  @Test
  void delete_notFound_throwsResourceNotFoundException() {
    when(vendorRepository.findById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vendorService.delete(7L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(vendorRepository, never()).delete(any());
  }

  @Test
  void updateVerification_found_updatesStatusAndSaves() {
    Vendor existing = buildVendor(1L);
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

    VerificationUpdateRequest request = new VerificationUpdateRequest();
    request.setStatus(VerificationStatus.VERIFIED);

    VendorResponse response = vendorService.updateVerification(1L, request);

    assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    assertThat(existing.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    verify(vendorRepository).save(existing);
  }

  @Test
  void updateVerification_notFound_throwsResourceNotFoundException() {
    when(vendorRepository.findById(5L)).thenReturn(Optional.empty());
    VerificationUpdateRequest request = new VerificationUpdateRequest();
    request.setStatus(VerificationStatus.VERIFIED);

    assertThatThrownBy(() -> vendorService.updateVerification(5L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateRating_found_updatesRatingAndSaves() {
    Vendor existing = buildVendor(1L);
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

    RatingUpdateRequest request = new RatingUpdateRequest();
    request.setRating(new BigDecimal("4.75"));

    VendorResponse response = vendorService.updateRating(1L, request);

    assertThat(response.getRating()).isEqualByComparingTo(new BigDecimal("4.75"));
    verify(vendorRepository).save(existing);
  }

  @Test
  void updateRating_notFound_throwsResourceNotFoundException() {
    when(vendorRepository.findById(11L)).thenReturn(Optional.empty());
    RatingUpdateRequest request = new RatingUpdateRequest();
    request.setRating(new BigDecimal("3.0"));

    assertThatThrownBy(() -> vendorService.updateRating(11L, request))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(vendorRepository, never()).save(any());
  }

  @Test
  void findVendor_found_returnsEntity() {
    Vendor existing = buildVendor(1L);
    when(vendorRepository.findById(1L)).thenReturn(Optional.of(existing));

    Vendor result = vendorService.findVendor(1L);

    assertThat(result).isSameAs(existing);
  }

  @Test
  void findVendor_notFound_throwsWithHelpfulMessage() {
    when(vendorRepository.findById(123L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> vendorService.findVendor(123L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Vendor not found with id 123");
  }
}
