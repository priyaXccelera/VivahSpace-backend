package com.example.proposalservice.client;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class VendorServiceClient {

  private static final String BASE_URL = "http://vendor-service/api/v1/vendors";

  private final RestTemplate restTemplate;

  public VendorServiceClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public CommissionDto recordCommission(Long vendorId, Long bookingId, BigDecimal amount) {
    Map<String, Object> body = new HashMap<>();
    body.put("bookingId", bookingId);
    body.put("amount", amount);
    return restTemplate.postForObject(
        BASE_URL + "/{vendorId}/commissions", body, CommissionDto.class, vendorId);
  }
}
