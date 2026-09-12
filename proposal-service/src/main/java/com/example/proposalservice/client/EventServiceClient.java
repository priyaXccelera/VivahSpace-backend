package com.example.proposalservice.client;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EventServiceClient {

  private static final String BASE_URL = "http://event-service/api/v1";

  private final RestTemplate restTemplate;

  public EventServiceClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public EventDto createEvent(Long proposalId, Long hallId, Long userId, Integer guestCount) {
    Map<String, Object> body = new HashMap<>();
    body.put("proposalId", proposalId);
    body.put("hallId", hallId);
    body.put("userId", userId);
    body.put("guestCount", guestCount);
    return restTemplate.postForObject(BASE_URL + "/events", body, EventDto.class);
  }
}
