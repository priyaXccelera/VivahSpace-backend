package com.example.proposalservice.service;

import com.example.proposalservice.client.BookingDto;
import com.example.proposalservice.client.EventDto;
import com.example.proposalservice.client.EventServiceClient;
import com.example.proposalservice.client.HallDto;
import com.example.proposalservice.client.HallServiceClient;
import com.example.proposalservice.client.VendorServiceClient;
import com.example.proposalservice.dto.*;
import com.example.proposalservice.entity.*;
import com.example.proposalservice.exception.InvalidCouponException;
import com.example.proposalservice.exception.InvalidProposalStateException;
import com.example.proposalservice.exception.ResourceNotFoundException;
import com.example.proposalservice.repository.CouponRepository;
import com.example.proposalservice.repository.ProposalItemRepository;
import com.example.proposalservice.repository.ProposalRepository;
import com.example.proposalservice.repository.SeasonalPricingRuleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalService {

  private final ProposalRepository proposalRepository;
  private final ProposalItemRepository proposalItemRepository;
  private final SeasonalPricingRuleRepository seasonalPricingRuleRepository;
  private final CouponRepository couponRepository;
  private final HallServiceClient hallServiceClient;
  private final EventServiceClient eventServiceClient;
  private final VendorServiceClient vendorServiceClient;

  public ProposalService(
      ProposalRepository proposalRepository,
      ProposalItemRepository proposalItemRepository,
      SeasonalPricingRuleRepository seasonalPricingRuleRepository,
      CouponRepository couponRepository,
      HallServiceClient hallServiceClient,
      EventServiceClient eventServiceClient,
      VendorServiceClient vendorServiceClient) {
    this.proposalRepository = proposalRepository;
    this.proposalItemRepository = proposalItemRepository;
    this.seasonalPricingRuleRepository = seasonalPricingRuleRepository;
    this.couponRepository = couponRepository;
    this.hallServiceClient = hallServiceClient;
    this.eventServiceClient = eventServiceClient;
    this.vendorServiceClient = vendorServiceClient;
  }

  @Transactional
  public ProposalResponse create(ProposalRequest request) {
    HallDto hall = hallServiceClient.getHall(request.getHallId());
    Proposal proposal = new Proposal();
    proposal.setUserId(request.getUserId());
    proposal.setHallId(request.getHallId());
    proposal.setHallBasePrice(hall.getBasePrice());
    proposal.setStatus(ProposalStatus.DRAFT);
    Proposal saved = proposalRepository.save(proposal);
    recomputeTotal(saved);
    return ProposalResponse.from(proposalRepository.save(saved));
  }

  @Transactional(readOnly = true)
  public Page<ProposalResponse> list(Long userId, Pageable pageable) {
    Page<Proposal> page =
        userId != null
            ? proposalRepository.findByUserId(userId, pageable)
            : proposalRepository.findAll(pageable);
    return page.map(ProposalResponse::from);
  }

  @Transactional(readOnly = true)
  public ProposalResponse getById(Long id) {
    return ProposalResponse.from(findProposal(id));
  }

  @Transactional
  public void delete(Long id) {
    proposalRepository.delete(findProposal(id));
  }

  @Transactional
  public ProposalResponse addItem(Long proposalId, ProposalItemRequest request) {
    Proposal proposal = findProposal(proposalId);
    ProposalItem item = new ProposalItem();
    item.setProposal(proposal);
    item.setVendorId(request.getVendorId());
    item.setServiceType(request.getServiceType());
    item.setPrice(request.getPrice());
    proposal.getItems().add(item);
    recomputeTotal(proposal);
    return ProposalResponse.from(proposalRepository.save(proposal));
  }

  @Transactional
  public ProposalResponse removeItem(Long proposalId, Long itemId) {
    Proposal proposal = findProposal(proposalId);
    ProposalItem item =
        proposal.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(
                () -> new ResourceNotFoundException("Proposal item not found with id " + itemId));
    proposal.getItems().remove(item);
    recomputeTotal(proposal);
    return ProposalResponse.from(proposalRepository.save(proposal));
  }

  @Transactional
  public ProposalResponse applyCoupon(Long proposalId, CouponApplyRequest request) {
    Proposal proposal = findProposal(proposalId);
    Coupon coupon =
        couponRepository
            .findByCodeIgnoreCase(request.getCode())
            .orElseThrow(
                () -> new InvalidCouponException("Coupon not found: " + request.getCode()));
    if (!coupon.isActive()) {
      throw new InvalidCouponException("Coupon is not active: " + request.getCode());
    }
    if (coupon.getExpiryDate().isBefore(LocalDate.now())) {
      throw new InvalidCouponException("Coupon has expired: " + request.getCode());
    }
    proposal.setCouponCode(coupon.getCode());
    recomputeTotal(proposal);
    return ProposalResponse.from(proposalRepository.save(proposal));
  }

  @Transactional
  public ProposalResponse share(Long proposalId) {
    Proposal proposal = findProposal(proposalId);
    proposal.setStatus(ProposalStatus.SHARED);
    return ProposalResponse.from(proposalRepository.save(proposal));
  }

  @Transactional
  public BookProposalResponse book(Long proposalId, BookProposalRequest request) {
    Proposal proposal = findProposal(proposalId);
    if (proposal.getStatus() == ProposalStatus.CONFIRMED) {
      throw new InvalidProposalStateException("Proposal " + proposalId + " is already confirmed");
    }
    BookingDto booking =
        hallServiceClient.bookSlot(request.getSlotId(), proposal.getUserId(), proposal.getId());
    proposal.setStatus(ProposalStatus.CONFIRMED);
    Proposal saved = proposalRepository.save(proposal);

    if (booking != null) {
      for (ProposalItem item : saved.getItems()) {
        try {
          vendorServiceClient.recordCommission(
              item.getVendorId(), booking.getId(), item.getPrice());
        } catch (RuntimeException ex) {
          // best-effort: commission tracking must not block the booking itself
        }
      }
    }

    EventDto event =
        eventServiceClient.createEvent(
            saved.getId(), saved.getHallId(), saved.getUserId(), request.getGuestCount());

    return new BookProposalResponse(
        ProposalResponse.from(saved),
        booking != null ? booking.getId() : null,
        event != null ? event.getId() : null);
  }

  @Transactional(readOnly = true)
  public RevenueAnalyticsResponse revenue() {
    List<Proposal> confirmed = proposalRepository.findByStatus(ProposalStatus.CONFIRMED);
    BigDecimal hallRevenue =
        confirmed.stream()
            .map(Proposal::getHallBasePrice)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal vendorRevenue =
        confirmed.stream()
            .flatMap(p -> p.getItems().stream())
            .map(ProposalItem::getPrice)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalRevenue =
        confirmed.stream()
            .map(Proposal::getEstimatedTotal)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new RevenueAnalyticsResponse(confirmed.size(), totalRevenue, hallRevenue, vendorRevenue);
  }

  private void recomputeTotal(Proposal proposal) {
    BigDecimal base =
        proposal.getHallBasePrice() != null ? proposal.getHallBasePrice() : BigDecimal.ZERO;
    BigDecimal itemsTotal =
        proposal.getItems().stream()
            .map(ProposalItem::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal subtotal = base.add(itemsTotal);

    BigDecimal multiplier = findActiveSeasonalMultiplier();
    BigDecimal afterSeasonal = subtotal.multiply(multiplier);

    BigDecimal total = afterSeasonal;
    if (proposal.getCouponCode() != null) {
      total =
          couponRepository
              .findByCodeIgnoreCase(proposal.getCouponCode())
              .filter(Coupon::isActive)
              .filter(c -> !c.getExpiryDate().isBefore(LocalDate.now()))
              .map(
                  c -> {
                    BigDecimal discount =
                        afterSeasonal
                            .multiply(c.getDiscountPercent())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    return afterSeasonal.subtract(discount);
                  })
              .orElse(afterSeasonal);
    }
    proposal.setEstimatedTotal(total.setScale(2, RoundingMode.HALF_UP));
  }

  private BigDecimal findActiveSeasonalMultiplier() {
    LocalDate today = LocalDate.now();
    List<SeasonalPricingRule> rules =
        seasonalPricingRuleRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            today, today);
    return rules.stream()
        .findFirst()
        .map(SeasonalPricingRule::getMultiplier)
        .orElse(BigDecimal.ONE);
  }

  private Proposal findProposal(Long id) {
    return proposalRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Proposal not found with id " + id));
  }
}
