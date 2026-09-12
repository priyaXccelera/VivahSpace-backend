package com.example.proposalservice.service;

import com.example.proposalservice.dto.CouponRequest;
import com.example.proposalservice.dto.CouponResponse;
import com.example.proposalservice.entity.Coupon;
import com.example.proposalservice.repository.CouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

  private final CouponRepository repository;

  public CouponService(CouponRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public CouponResponse create(CouponRequest request) {
    Coupon coupon = new Coupon();
    coupon.setCode(request.getCode());
    coupon.setDiscountPercent(request.getDiscountPercent());
    coupon.setExpiryDate(request.getExpiryDate());
    coupon.setActive(request.getActive() == null || request.getActive());
    return CouponResponse.from(repository.save(coupon));
  }

  @Transactional(readOnly = true)
  public Page<CouponResponse> list(Pageable pageable) {
    return repository.findAll(pageable).map(CouponResponse::from);
  }
}
