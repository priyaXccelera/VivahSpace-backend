package com.example.proposalservice.repository;

import com.example.proposalservice.entity.SeasonalPricingRule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonalPricingRuleRepository extends JpaRepository<SeasonalPricingRule, Long> {
  Page<SeasonalPricingRule> findAll(Pageable pageable);

  List<SeasonalPricingRule> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
      LocalDate date1, LocalDate date2);
}
