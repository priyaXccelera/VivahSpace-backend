package com.example.proposalservice.service;

import com.example.proposalservice.dto.SeasonalPricingRequest;
import com.example.proposalservice.dto.SeasonalPricingResponse;
import com.example.proposalservice.entity.SeasonalPricingRule;
import com.example.proposalservice.repository.SeasonalPricingRuleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonalPricingService {

  private final SeasonalPricingRuleRepository repository;

  public SeasonalPricingService(SeasonalPricingRuleRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public SeasonalPricingResponse create(SeasonalPricingRequest request) {
    SeasonalPricingRule rule = new SeasonalPricingRule();
    rule.setName(request.getName());
    rule.setStartDate(request.getStartDate());
    rule.setEndDate(request.getEndDate());
    rule.setMultiplier(request.getMultiplier());
    return SeasonalPricingResponse.from(repository.save(rule));
  }

  @Transactional(readOnly = true)
  public Page<SeasonalPricingResponse> list(Pageable pageable) {
    return repository.findAll(pageable).map(SeasonalPricingResponse::from);
  }
}
