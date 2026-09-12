package com.example.proposalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.proposalservice.client.BookingDto;
import com.example.proposalservice.client.EventDto;
import com.example.proposalservice.client.EventServiceClient;
import com.example.proposalservice.client.HallDto;
import com.example.proposalservice.client.HallServiceClient;
import com.example.proposalservice.client.VendorServiceClient;
import com.example.proposalservice.dto.BookProposalRequest;
import com.example.proposalservice.dto.BookProposalResponse;
import com.example.proposalservice.dto.CouponApplyRequest;
import com.example.proposalservice.dto.ProposalItemRequest;
import com.example.proposalservice.dto.ProposalRequest;
import com.example.proposalservice.dto.ProposalResponse;
import com.example.proposalservice.entity.Coupon;
import com.example.proposalservice.entity.Proposal;
import com.example.proposalservice.entity.ProposalItem;
import com.example.proposalservice.entity.ProposalStatus;
import com.example.proposalservice.entity.ServiceType;
import com.example.proposalservice.exception.InvalidCouponException;
import com.example.proposalservice.exception.InvalidProposalStateException;
import com.example.proposalservice.exception.ResourceNotFoundException;
import com.example.proposalservice.repository.CouponRepository;
import com.example.proposalservice.repository.ProposalItemRepository;
import com.example.proposalservice.repository.ProposalRepository;
import com.example.proposalservice.repository.SeasonalPricingRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

  @Mock private ProposalRepository proposalRepository;
  @Mock private ProposalItemRepository proposalItemRepository;
  @Mock private SeasonalPricingRuleRepository seasonalPricingRuleRepository;
  @Mock private CouponRepository couponRepository;
  @Mock private HallServiceClient hallServiceClient;
  @Mock private EventServiceClient eventServiceClient;
  @Mock private VendorServiceClient vendorServiceClient;

  @InjectMocks private ProposalService proposalService;

  @BeforeEach
  void setUp() {
    // Default: no active seasonal rule (multiplier neutral) unless a test overrides it.
    lenient()
        .when(
            seasonalPricingRuleRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any()))
        .thenReturn(Collections.emptyList());
    // save() echoes back the same instance, mimicking JPA save-and-return semantics
    lenient()
        .when(proposalRepository.save(any(Proposal.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private Proposal newProposal(Long id, BigDecimal hallBasePrice, ProposalStatus status) {
    Proposal p = new Proposal();
    p.setId(id);
    p.setUserId(2L);
    p.setHallId(1L);
    p.setHallBasePrice(hallBasePrice);
    p.setStatus(status);
    p.setEstimatedTotal(hallBasePrice);
    p.setItems(new ArrayList<>());
    return p;
  }

  // ---------- create() ----------

  @Test
  void create_callsHallServiceClientAndBuildsProposalWithHallBasePrice() {
    ProposalRequest request = new ProposalRequest();
    request.setUserId(2L);
    request.setHallId(1L);

    HallDto hall = new HallDto();
    hall.setId(1L);
    hall.setName("Grand Regency Hall");
    hall.setBasePrice(new BigDecimal("250000.00"));
    when(hallServiceClient.getHall(1L)).thenReturn(hall);

    ProposalResponse response = proposalService.create(request);

    verify(hallServiceClient, times(1)).getHall(1L);
    assertEquals(new BigDecimal("250000.00"), response.getHallBasePrice());
    assertEquals(2L, response.getUserId());
    assertEquals(1L, response.getHallId());
    assertEquals(ProposalStatus.DRAFT, response.getStatus());
    assertEquals(0, new BigDecimal("250000.00").compareTo(response.getEstimatedTotal()));
    verify(proposalRepository, times(2)).save(any(Proposal.class));
  }

  @Test
  void create_propagatesResourceNotFoundWhenHallMissing() {
    ProposalRequest request = new ProposalRequest();
    request.setUserId(2L);
    request.setHallId(999L);
    when(hallServiceClient.getHall(999L))
        .thenThrow(new ResourceNotFoundException("Hall not found with id 999"));

    assertThrows(ResourceNotFoundException.class, () -> proposalService.create(request));
    verify(proposalRepository, never()).save(any());
  }

  // ---------- addItem() / removeItem() ----------

  @Test
  void addItem_recomputesEstimatedTotalIncludingNewItem() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    ProposalItemRequest itemRequest = new ProposalItemRequest();
    itemRequest.setVendorId(1L);
    itemRequest.setServiceType(ServiceType.PHOTOGRAPHY);
    itemRequest.setPrice(new BigDecimal("50000.00"));

    ProposalResponse response = proposalService.addItem(1L, itemRequest);

    assertEquals(1, response.getItems().size());
    assertEquals(0, new BigDecimal("300000.00").compareTo(response.getEstimatedTotal()));
  }

  @Test
  void addItem_throwsResourceNotFoundWhenProposalMissing() {
    when(proposalRepository.findById(42L)).thenReturn(Optional.empty());
    ProposalItemRequest itemRequest = new ProposalItemRequest();
    itemRequest.setVendorId(1L);
    itemRequest.setServiceType(ServiceType.DJ);
    itemRequest.setPrice(BigDecimal.TEN);

    assertThrows(ResourceNotFoundException.class, () -> proposalService.addItem(42L, itemRequest));
  }

  @Test
  void removeItem_recomputesEstimatedTotalExcludingRemovedItem() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    ProposalItem item1 = new ProposalItem();
    item1.setId(10L);
    item1.setProposal(proposal);
    item1.setVendorId(1L);
    item1.setServiceType(ServiceType.PHOTOGRAPHY);
    item1.setPrice(new BigDecimal("50000.00"));
    ProposalItem item2 = new ProposalItem();
    item2.setId(11L);
    item2.setProposal(proposal);
    item2.setVendorId(2L);
    item2.setServiceType(ServiceType.CATERING);
    item2.setPrice(new BigDecimal("30000.00"));
    proposal.getItems().add(item1);
    proposal.getItems().add(item2);
    proposal.setEstimatedTotal(new BigDecimal("330000.00"));

    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    ProposalResponse response = proposalService.removeItem(1L, 10L);

    assertEquals(1, response.getItems().size());
    assertEquals(11L, response.getItems().get(0).getId());
    assertEquals(0, new BigDecimal("280000.00").compareTo(response.getEstimatedTotal()));
  }

  @Test
  void removeItem_throwsResourceNotFoundWhenItemMissing() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    assertThrows(ResourceNotFoundException.class, () -> proposalService.removeItem(1L, 999L));
  }

  // ---------- applyCoupon() ----------

  @Test
  void applyCoupon_rejectsUnknownCoupon() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
    when(couponRepository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

    CouponApplyRequest request = new CouponApplyRequest();
    request.setCode("UNKNOWN");

    assertThrows(InvalidCouponException.class, () -> proposalService.applyCoupon(1L, request));
  }

  @Test
  void applyCoupon_rejectsInactiveCoupon() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    Coupon coupon = new Coupon();
    coupon.setCode("INACTIVE10");
    coupon.setActive(false);
    coupon.setDiscountPercent(new BigDecimal("10.00"));
    coupon.setExpiryDate(LocalDate.now().plusDays(30));
    when(couponRepository.findByCodeIgnoreCase("INACTIVE10")).thenReturn(Optional.of(coupon));

    CouponApplyRequest request = new CouponApplyRequest();
    request.setCode("INACTIVE10");

    assertThrows(InvalidCouponException.class, () -> proposalService.applyCoupon(1L, request));
  }

  @Test
  void applyCoupon_rejectsExpiredCoupon() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    Coupon coupon = new Coupon();
    coupon.setCode("EXPIRED10");
    coupon.setActive(true);
    coupon.setDiscountPercent(new BigDecimal("10.00"));
    coupon.setExpiryDate(LocalDate.now().minusDays(1));
    when(couponRepository.findByCodeIgnoreCase("EXPIRED10")).thenReturn(Optional.of(coupon));

    CouponApplyRequest request = new CouponApplyRequest();
    request.setCode("EXPIRED10");

    assertThrows(InvalidCouponException.class, () -> proposalService.applyCoupon(1L, request));
  }

  @Test
  void applyCoupon_acceptsValidCouponAndComputesDiscountCorrectly() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    Coupon coupon = new Coupon();
    coupon.setCode("WELCOME10");
    coupon.setActive(true);
    coupon.setDiscountPercent(new BigDecimal("10.00"));
    coupon.setExpiryDate(LocalDate.now().plusDays(30));
    // applyCoupon() looks it up once, then recomputeTotal() looks it up again to apply the discount
    when(couponRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(coupon));

    CouponApplyRequest request = new CouponApplyRequest();
    request.setCode("WELCOME10");

    ProposalResponse response = proposalService.applyCoupon(1L, request);

    assertEquals("WELCOME10", response.getCouponCode());
    // 250000 * (1 - 0.10) = 225000.00
    assertEquals(0, new BigDecimal("225000.00").compareTo(response.getEstimatedTotal()));
  }

  // ---------- share() / book() state transitions ----------

  @Test
  void share_marksProposalAsShared() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    ProposalResponse response = proposalService.share(1L);

    assertEquals(ProposalStatus.SHARED, response.getStatus());
  }

  @Test
  void book_rejectsAlreadyConfirmedProposal() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.CONFIRMED);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    BookProposalRequest request = new BookProposalRequest();
    request.setSlotId(5L);
    request.setGuestCount(100);

    assertThrows(InvalidProposalStateException.class, () -> proposalService.book(1L, request));
    verify(hallServiceClient, never()).bookSlot(any(), any(), any());
    verify(vendorServiceClient, never()).recordCommission(any(), any(), any());
    verify(eventServiceClient, never()).createEvent(any(), any(), any(), any());
  }

  @Test
  void book_onSharedProposal_invokesHallVendorAndEventClientsAndConfirms() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.SHARED);
    ProposalItem item = new ProposalItem();
    item.setId(10L);
    item.setProposal(proposal);
    item.setVendorId(1L);
    item.setServiceType(ServiceType.PHOTOGRAPHY);
    item.setPrice(new BigDecimal("50000.00"));
    proposal.getItems().add(item);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    BookingDto booking = new BookingDto();
    booking.setId(500L);
    booking.setHallId(1L);
    booking.setUserId(2L);
    when(hallServiceClient.bookSlot(eq(5L), eq(2L), eq(1L))).thenReturn(booking);

    EventDto event = new EventDto();
    event.setId(900L);
    event.setHallId(1L);
    event.setUserId(2L);
    when(eventServiceClient.createEvent(eq(1L), eq(1L), eq(2L), eq(100))).thenReturn(event);

    BookProposalRequest request = new BookProposalRequest();
    request.setSlotId(5L);
    request.setGuestCount(100);

    BookProposalResponse response = proposalService.book(1L, request);

    assertEquals(ProposalStatus.CONFIRMED, response.getProposal().getStatus());
    assertEquals(500L, response.getBookingId());
    assertEquals(900L, response.getEventId());

    verify(hallServiceClient, times(1)).bookSlot(5L, 2L, 1L);
    verify(vendorServiceClient, times(1)).recordCommission(1L, 500L, new BigDecimal("50000.00"));
    verify(eventServiceClient, times(1)).createEvent(1L, 1L, 2L, 100);
  }

  @Test
  void book_recordsCommissionForEachItem() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    ProposalItem item1 = new ProposalItem();
    item1.setId(10L);
    item1.setProposal(proposal);
    item1.setVendorId(1L);
    item1.setServiceType(ServiceType.PHOTOGRAPHY);
    item1.setPrice(new BigDecimal("50000.00"));
    ProposalItem item2 = new ProposalItem();
    item2.setId(11L);
    item2.setProposal(proposal);
    item2.setVendorId(2L);
    item2.setServiceType(ServiceType.CATERING);
    item2.setPrice(new BigDecimal("30000.00"));
    proposal.getItems().add(item1);
    proposal.getItems().add(item2);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

    BookingDto booking = new BookingDto();
    booking.setId(777L);
    when(hallServiceClient.bookSlot(any(), any(), any())).thenReturn(booking);
    when(eventServiceClient.createEvent(any(), any(), any(), any())).thenReturn(new EventDto());

    BookProposalRequest request = new BookProposalRequest();
    request.setSlotId(5L);
    request.setGuestCount(50);

    proposalService.book(1L, request);

    verify(vendorServiceClient, times(1)).recordCommission(1L, 777L, new BigDecimal("50000.00"));
    verify(vendorServiceClient, times(1)).recordCommission(2L, 777L, new BigDecimal("30000.00"));
    verify(vendorServiceClient, times(2)).recordCommission(any(), any(), any());
  }

  @Test
  void book_skipsCommissionWhenHallBookingReturnsNull() {
    Proposal proposal = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.DRAFT);
    ProposalItem item = new ProposalItem();
    item.setId(10L);
    item.setProposal(proposal);
    item.setVendorId(1L);
    item.setServiceType(ServiceType.DJ);
    item.setPrice(new BigDecimal("20000.00"));
    proposal.getItems().add(item);
    when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
    when(hallServiceClient.bookSlot(any(), any(), any())).thenReturn(null);
    when(eventServiceClient.createEvent(any(), any(), any(), any())).thenReturn(new EventDto());

    BookProposalRequest request = new BookProposalRequest();
    request.setSlotId(5L);
    request.setGuestCount(20);

    BookProposalResponse response = proposalService.book(1L, request);

    assertEquals(null, response.getBookingId());
    verify(vendorServiceClient, never()).recordCommission(any(), any(), any());
  }

  @Test
  void book_notFoundProposal_throwsResourceNotFoundException() {
    when(proposalRepository.findById(404L)).thenReturn(Optional.empty());
    BookProposalRequest request = new BookProposalRequest();
    request.setSlotId(1L);
    request.setGuestCount(10);

    assertThrows(ResourceNotFoundException.class, () -> proposalService.book(404L, request));
  }

  // ---------- revenue() ----------

  @Test
  void revenue_sumsOnlyConfirmedProposals() {
    Proposal confirmed = newProposal(1L, new BigDecimal("250000.00"), ProposalStatus.CONFIRMED);
    confirmed.setEstimatedTotal(new BigDecimal("275000.00"));
    ProposalItem item = new ProposalItem();
    item.setPrice(new BigDecimal("25000.00"));
    confirmed.getItems().add(item);

    when(proposalRepository.findByStatus(ProposalStatus.CONFIRMED)).thenReturn(List.of(confirmed));

    var response = proposalService.revenue();

    assertEquals(1, response.getConfirmedBookings());
    assertEquals(0, new BigDecimal("275000.00").compareTo(response.getTotalRevenue()));
    assertEquals(0, new BigDecimal("250000.00").compareTo(response.getHallRevenue()));
    assertEquals(0, new BigDecimal("25000.00").compareTo(response.getVendorRevenue()));
  }
}
