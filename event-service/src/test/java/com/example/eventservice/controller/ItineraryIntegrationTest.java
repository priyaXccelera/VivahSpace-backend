package com.example.eventservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.dto.ItineraryEntryRequest;
import com.example.eventservice.dto.ItineraryEntryResponse;
import com.example.eventservice.support.TestAuthClient;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Full-stack integration tests for the itinerary sub-resource of an event
 * (/api/v1/events/{eventId}/itinerary). Runs against the real, already-running event-service Spring
 * context and the real shared Postgres database - no mocks, no H2, no Testcontainers.
 *
 * <p>A dedicated event is created by this test class for itinerary manipulation and torn down
 * afterwards; the seeded demo event/itinerary rows (ids 1-2 from data.sql) are never touched.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItineraryIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private String accessToken;
  private Long ownedEventId;
  private final List<Long> createdEntryIds = new ArrayList<>();

  @BeforeAll
  void setUp() {
    TestAuthClient.AuthResult authResult = TestAuthClient.registerAndLogin();
    this.accessToken = authResult.accessToken;
    Long userId = authResult.userId != null ? authResult.userId : 123456789L;

    EventRequest eventRequest = new EventRequest();
    eventRequest.setProposalId(9301L);
    eventRequest.setHallId(9302L);
    eventRequest.setUserId(userId);
    eventRequest.setGuestCount(90);

    ResponseEntity<EventResponse> response =
        restTemplate.exchange(
            "/api/v1/events",
            HttpMethod.POST,
            new HttpEntity<>(eventRequest, authHeaders()),
            EventResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    this.ownedEventId = response.getBody().getId();
  }

  @AfterEach
  void cleanupRemainingEntries() {
    for (Long id : new ArrayList<>(createdEntryIds)) {
      restTemplate.exchange(
          "/api/v1/events/{eventId}/itinerary/{entryId}",
          HttpMethod.DELETE,
          new HttpEntity<>(authHeaders()),
          Void.class,
          ownedEventId,
          id);
    }
    createdEntryIds.clear();
  }

  @AfterAll
  void tearDownEvent() {
    if (ownedEventId != null) {
      restTemplate.exchange(
          "/api/v1/events/{id}",
          HttpMethod.DELETE,
          new HttpEntity<>(authHeaders()),
          Void.class,
          ownedEventId);
    }
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private ItineraryEntryRequest buildRequest(
      String title, LocalTime start, LocalTime end, String description) {
    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle(title);
    request.setStartTime(start);
    request.setEndTime(end);
    request.setDescription(description);
    return request;
  }

  @Test
  void createListUpdateDeleteItineraryEntry_worksEndToEnd() {
    // CREATE
    ResponseEntity<ItineraryEntryResponse> createResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary",
            HttpMethod.POST,
            new HttpEntity<>(
                buildRequest(
                    "Welcome Drinks", LocalTime.of(16, 0), LocalTime.of(17, 0), "Guests arrive"),
                authHeaders()),
            ItineraryEntryResponse.class,
            ownedEventId);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ItineraryEntryResponse created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.getId()).isNotNull();
    assertThat(created.getEventId()).isEqualTo(ownedEventId);
    assertThat(created.getTitle()).isEqualTo("Welcome Drinks");
    assertThat(created.getStartTime()).isEqualTo(LocalTime.of(16, 0));
    assertThat(created.getEndTime()).isEqualTo(LocalTime.of(17, 0));

    Long entryId = created.getId();
    createdEntryIds.add(entryId);

    // LIST
    ResponseEntity<String> listResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class,
            ownedEventId);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).contains("\"id\":" + entryId);

    // UPDATE - shift the time window later
    ResponseEntity<ItineraryEntryResponse> updateResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary/{entryId}",
            HttpMethod.PUT,
            new HttpEntity<>(
                buildRequest(
                    "Welcome Drinks (delayed)",
                    LocalTime.of(16, 30),
                    LocalTime.of(17, 30),
                    "Delayed start"),
                authHeaders()),
            ItineraryEntryResponse.class,
            ownedEventId,
            entryId);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody()).isNotNull();
    assertThat(updateResponse.getBody().getTitle()).isEqualTo("Welcome Drinks (delayed)");
    assertThat(updateResponse.getBody().getStartTime()).isEqualTo(LocalTime.of(16, 30));
    assertThat(updateResponse.getBody().getEndTime()).isEqualTo(LocalTime.of(17, 30));
    assertThat(updateResponse.getBody().getEndTime())
        .isAfter(updateResponse.getBody().getStartTime());

    // DELETE
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary/{entryId}",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            Void.class,
            ownedEventId,
            entryId);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    createdEntryIds.remove(entryId);

    // Subsequent update on deleted entry -> 404
    ResponseEntity<String> updateAfterDelete =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary/{entryId}",
            HttpMethod.PUT,
            new HttpEntity<>(
                buildRequest("Ghost entry", LocalTime.of(1, 0), null, null), authHeaders()),
            String.class,
            ownedEventId,
            entryId);
    assertThat(updateAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createItineraryEntry_withBlankTitle_returns400BadRequest() {
    ItineraryEntryRequest invalidRequest = buildRequest("", LocalTime.of(10, 0), null, null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary",
            HttpMethod.POST,
            new HttpEntity<>(invalidRequest, authHeaders()),
            String.class,
            ownedEventId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void createItineraryEntry_withMissingStartTime_returns400BadRequest() {
    ItineraryEntryRequest invalidRequest =
        buildRequest("Missing start", null, LocalTime.of(12, 0), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary",
            HttpMethod.POST,
            new HttpEntity<>(invalidRequest, authHeaders()),
            String.class,
            ownedEventId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void createItineraryEntry_forNonExistentEvent_returns404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/itinerary",
            HttpMethod.POST,
            new HttpEntity<>(
                buildRequest("Orphan entry", LocalTime.of(9, 0), null, null), authHeaders()),
            String.class,
            987654321L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createItineraryEntry_withoutAuthentication_isRejected() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/events/" + ownedEventId + "/itinerary",
            new HttpEntity<>(buildRequest("No Auth", LocalTime.of(9, 0), null, null)),
            String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }
}
