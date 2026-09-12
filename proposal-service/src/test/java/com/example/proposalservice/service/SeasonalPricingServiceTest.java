package com.example.proposalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.proposalservice.dto.SeasonalPricingRequest;
import com.example.proposalservice.dto.SeasonalPricingResponse;
import com.example.proposalservice.entity.SeasonalPricingRule;
import com.example.proposalservice.repository.SeasonalPricingRuleRepository;
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
class SeasonalPricingServiceTest {

  @Mock private SeasonalPricingRuleRepository repository;

  @InjectMocks private SeasonalPricingService seasonalPricingService;

  @Test
  void create_savesRuleWithGivenFields() {
    SeasonalPricingRequest request = new SeasonalPricingRequest();
    request.setName("Summer Peak");
    request.setStartDate(LocalDate.now());
    request.setEndDate(LocalDate.now().plusDays(30));
    request.setMultiplier(new BigDecimal("1.20"));

    when(repository.save(any(SeasonalPricingRule.class)))
        .thenAnswer(
            inv -> {
              SeasonalPricingRule r = inv.getArgument(0);
              r.setId(5L);
              return r;
            });

    SeasonalPricingResponse response = seasonalPricingService.create(request);

    ArgumentCaptor<SeasonalPricingRule> captor = ArgumentCaptor.forClass(SeasonalPricingRule.class);
    verify(repository).save(captor.capture());
    assertEquals("Summer Peak", captor.getValue().getName());
    assertEquals(0, new BigDecimal("1.20").compareTo(captor.getValue().getMultiplier()));
    assertEquals(5L, response.getId());
    assertEquals("Summer Peak", response.getName());
  }

  @Test
  void list_returnsPagedRules() {
    SeasonalPricingRule rule = new SeasonalPricingRule();
    rule.setId(1L);
    rule.setName("Wedding Season Peak");
    rule.setStartDate(LocalDate.now());
    rule.setEndDate(LocalDate.now().plusDays(180));
    rule.setMultiplier(new BigDecimal("1.15"));

    Pageable pageable = PageRequest.of(0, 20);
    Page<SeasonalPricingRule> page = new PageImpl<>(List.of(rule), pageable, 1);
    when(repository.findAll(pageable)).thenReturn(page);

    Page<SeasonalPricingResponse> result = seasonalPricingService.list(pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals("Wedding Season Peak", result.getContent().get(0).getName());
  }
}
