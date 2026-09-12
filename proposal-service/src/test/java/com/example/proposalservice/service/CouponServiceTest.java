package com.example.proposalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.proposalservice.dto.CouponRequest;
import com.example.proposalservice.dto.CouponResponse;
import com.example.proposalservice.entity.Coupon;
import com.example.proposalservice.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

  @Mock private CouponRepository repository;

  @InjectMocks private CouponService couponService;

  @Test
  void create_savesCouponWithDefaultActiveTrueWhenActiveNotProvided() {
    CouponRequest request = new CouponRequest();
    request.setCode("NEWCODE");
    request.setDiscountPercent(new BigDecimal("15.00"));
    request.setExpiryDate(LocalDate.now().plusDays(60));
    request.setActive(null);

    when(repository.save(any(Coupon.class)))
        .thenAnswer(
            inv -> {
              Coupon c = inv.getArgument(0);
              c.setId(1L);
              return c;
            });

    CouponResponse response = couponService.create(request);

    ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
    verify(repository).save(captor.capture());
    assertTrue(captor.getValue().isActive());
    assertEquals("NEWCODE", response.getCode());
    assertEquals(0, new BigDecimal("15.00").compareTo(response.getDiscountPercent()));
  }

  @Test
  void create_respectsExplicitActiveFalse() {
    CouponRequest request = new CouponRequest();
    request.setCode("DISABLED");
    request.setDiscountPercent(new BigDecimal("5.00"));
    request.setExpiryDate(LocalDate.now().plusDays(10));
    request.setActive(false);

    when(repository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

    CouponResponse response = couponService.create(request);

    assertFalse(response.isActive());
  }

  @Test
  void list_returnsPagedCoupons() {
    Coupon coupon = new Coupon();
    coupon.setId(1L);
    coupon.setCode("WELCOME10");
    coupon.setDiscountPercent(new BigDecimal("10.00"));
    coupon.setActive(true);
    coupon.setExpiryDate(LocalDate.now().plusDays(365));

    Pageable pageable = PageRequest.of(0, 20);
    Page<Coupon> page = new PageImpl<>(List.of(coupon), pageable, 1);
    when(repository.findAll(pageable)).thenReturn(page);

    Page<CouponResponse> result = couponService.list(pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals("WELCOME10", result.getContent().get(0).getCode());
  }
}
