package com.example.proposalservice.client;

import com.example.proposalservice.exception.ResourceNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class HallServiceClient {

  private static final String BASE_URL = "http://hall-service/api/v1";

  private final RestTemplate restTemplate;

  public HallServiceClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public HallDto getHall(Long hallId) {
    try {
      HallDto hall = restTemplate.getForObject(BASE_URL + "/halls/{id}", HallDto.class, hallId);
      if (hall == null) {
        throw new ResourceNotFoundException("Hall not found with id " + hallId);
      }
      return hall;
    } catch (HttpClientErrorException.NotFound e) {
      throw new ResourceNotFoundException("Hall not found with id " + hallId);
    }
  }

  public BookingDto bookSlot(Long slotId, Long userId, Long proposalId) {
    Map<String, Object> body = new HashMap<>();
    body.put("userId", userId);
    body.put("proposalId", proposalId);
    return restTemplate.postForObject(
        BASE_URL + "/slots/{slotId}/book", body, BookingDto.class, slotId);
  }
}
