package com.example.vendorservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.vendorservice.dto.PortfolioItemRequest;
import com.example.vendorservice.dto.PortfolioItemResponse;
import com.example.vendorservice.entity.PortfolioItem;
import com.example.vendorservice.entity.Vendor;
import com.example.vendorservice.exception.ResourceNotFoundException;
import com.example.vendorservice.repository.PortfolioItemRepository;
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
class PortfolioServiceTest {

  @Mock private PortfolioItemRepository portfolioItemRepository;

  @Mock private VendorService vendorService;

  private PortfolioService portfolioService;

  @BeforeEach
  void setUp() {
    portfolioService = new PortfolioService(portfolioItemRepository, vendorService);
  }

  private Vendor buildVendor(Long id) {
    Vendor vendor = new Vendor();
    vendor.setId(id);
    vendor.setBusinessName("Anita Photography");
    return vendor;
  }

  private PortfolioItemRequest buildRequest() {
    PortfolioItemRequest request = new PortfolioItemRequest();
    request.setTitle("Sharma-Reddy Wedding");
    request.setImageUrl("https://example.com/images/p1.jpg");
    request.setDescription("Candid coverage");
    return request;
  }

  private PortfolioItem buildItem(Long id, Vendor vendor) {
    PortfolioItem item = new PortfolioItem();
    item.setId(id);
    item.setVendor(vendor);
    item.setTitle("Sharma-Reddy Wedding");
    item.setImageUrl("https://example.com/images/p1.jpg");
    item.setDescription("Candid coverage");
    return item;
  }

  @Test
  void addItem_vendorExists_savesItemUnderVendor() {
    Vendor vendor = buildVendor(1L);
    when(vendorService.findVendor(1L)).thenReturn(vendor);
    PortfolioItem saved = buildItem(10L, vendor);
    when(portfolioItemRepository.save(any(PortfolioItem.class))).thenReturn(saved);

    PortfolioItemResponse response = portfolioService.addItem(1L, buildRequest());

    ArgumentCaptor<PortfolioItem> captor = ArgumentCaptor.forClass(PortfolioItem.class);
    verify(portfolioItemRepository).save(captor.capture());
    PortfolioItem passed = captor.getValue();
    assertThat(passed.getVendor()).isEqualTo(vendor);
    assertThat(passed.getTitle()).isEqualTo("Sharma-Reddy Wedding");

    assertThat(response.getId()).isEqualTo(10L);
    assertThat(response.getVendorId()).isEqualTo(1L);
    assertThat(response.getTitle()).isEqualTo("Sharma-Reddy Wedding");
  }

  @Test
  void addItem_vendorMissing_throwsResourceNotFoundAndNeverSaves() {
    when(vendorService.findVendor(99L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 99"));

    assertThatThrownBy(() -> portfolioService.addItem(99L, buildRequest()))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(portfolioItemRepository, never()).save(any());
  }

  @Test
  void list_vendorExists_returnsMappedPage() {
    Vendor vendor = buildVendor(1L);
    when(vendorService.findVendor(1L)).thenReturn(vendor);
    PortfolioItem item = buildItem(10L, vendor);
    Pageable pageable = PageRequest.of(0, 20);
    when(portfolioItemRepository.findByVendorId(eq(1L), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

    Page<PortfolioItemResponse> result = portfolioService.list(1L, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("Sharma-Reddy Wedding");
    verify(vendorService).findVendor(1L);
  }

  @Test
  void list_vendorMissing_throwsResourceNotFound() {
    Pageable pageable = PageRequest.of(0, 20);
    when(vendorService.findVendor(404L))
        .thenThrow(new ResourceNotFoundException("Vendor not found with id 404"));

    assertThatThrownBy(() -> portfolioService.list(404L, pageable))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(portfolioItemRepository, never()).findByVendorId(any(), any());
  }

  @Test
  void delete_itemBelongsToVendor_deletesItem() {
    Vendor vendor = buildVendor(1L);
    PortfolioItem item = buildItem(10L, vendor);
    when(portfolioItemRepository.findById(10L)).thenReturn(Optional.of(item));

    portfolioService.delete(1L, 10L);

    verify(portfolioItemRepository).delete(item);
  }

  @Test
  void delete_itemNotFound_throwsResourceNotFoundException() {
    when(portfolioItemRepository.findById(55L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> portfolioService.delete(1L, 55L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("55");
    verify(portfolioItemRepository, never()).delete(any());
  }

  @Test
  void delete_itemBelongsToDifferentVendor_throwsResourceNotFoundException() {
    Vendor otherVendor = buildVendor(2L);
    PortfolioItem item = buildItem(10L, otherVendor);
    when(portfolioItemRepository.findById(10L)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> portfolioService.delete(1L, 10L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("does not belong to vendor 1");
    verify(portfolioItemRepository, never()).delete(any());
  }
}
