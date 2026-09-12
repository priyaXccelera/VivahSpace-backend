package com.example.eventservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.dto.RsvpRequest;
import com.example.eventservice.dto.RsvpResponse;
import com.example.eventservice.entity.RsvpStatus;
import com.example.eventservice.support.TestAuthClient;
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
 * Full-stack integration tests for the RSVP sub-resource of an event
 * (/api/v1/events/{eventId}/rsvps). Runs against the real, already-running event-service Spring
 * context and the real shared Postgres database - no mocks, no H2, no Testcontainers.
 *
 * <p>A dedicated event is created by this test class for RSVP manipulation and torn down
 * afterwards; the seeded demo event/rsvp rows (ids 1-2 from data.sql) are never touched.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RsvpIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private String accessToken;
  private Long ownedEventId;
  private final List<Long> createdRsvpIds = new ArrayList<>();

  @BeforeAll
  void setUp() {
    TestAuthClient.AuthResult authResult = TestAuthClient.registerAndLogin();
    this.accessToken = authResult.accessToken;
    Long userId = authResult.userId != null ? authResult.userId : 123456789L;

    EventRequest eventRequest = new EventRequest();
    eventRequest.setProposalId(9101L);
    eventRequest.setHallId(9102L);
    eventRequest.setUserId(userId);
    eventRequest.setGuestCount(80);

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
  void cleanupRemainingRsvps() {
    for (Long id : new ArrayList<>(createdRsvpIds)) {
      restTemplate.exchange(
          "/api/v1/events/{eventId}/rsvps/{rsvpId}",
          HttpMethod.DELETE,
          new HttpEntity<>(authHeaders()),
          Void.class,
          ownedEventId,
          id);
    }
    createdRsvpIds.clear();
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

  private RsvpRequest buildRsvpRequest(String guestName, RsvpStatus status) {
    RsvpRequest request = new RsvpRequest();
    request.setGuestName(guestName);
    request.setContactInfo(guestName.toLowerCase().replace(" ", ".") + "@example.com");
    request.setStatus(status);
    return request;
  }

  @Test
  void createListUpdateDeleteRsvp_worksEndToEnd() {
    // CREATE
    ResponseEntity<RsvpResponse> createResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps",
            HttpMethod.POST,
            new HttpEntity<>(buildRsvpRequest("Test Guest One", null), authHeaders()),
            RsvpResponse.class,
            ownedEventId);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    RsvpResponse created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.getId()).isNotNull();
    assertThat(created.getEventId()).isEqualTo(ownedEventId);
    assertThat(created.getGuestName()).isEqualTo("Test Guest One");
    // status defaults to PENDING when not supplied
    assertThat(created.getStatus()).isEqualTo(RsvpStatus.PENDING);

    Long rsvpId = created.getId();
    createdRsvpIds.add(rsvpId);

    // LIST
    ResponseEntity<String> listResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class,
            ownedEventId);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).contains("\"id\":" + rsvpId);

    // UPDATE (status transition PENDING -> CONFIRMED)
    ResponseEntity<RsvpResponse> updateResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps/{rsvpId}",
            HttpMethod.PUT,
            new HttpEntity<>(
                buildRsvpRequest("Test Guest One Updated", RsvpStatus.CONFIRMED), authHeaders()),
            RsvpResponse.class,
            ownedEventId,
            rsvpId);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody()).isNotNull();
    assertThat(updateResponse.getBody().getGuestName()).isEqualTo("Test Guest One Updated");
    assertThat(updateResponse.getBody().getStatus()).isEqualTo(RsvpStatus.CONFIRMED);

    // DELETE
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps/{rsvpId}",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            Void.class,
            ownedEventId,
            rsvpId);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    createdRsvpIds.remove(rsvpId);

    // Subsequent update on deleted rsvp -> 404
    ResponseEntity<String> updateAfterDelete =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps/{rsvpId}",
            HttpMethod.PUT,
            new HttpEntity<>(buildRsvpRequest("Ghost", null), authHeaders()),
            String.class,
            ownedEventId,
            rsvpId);
    assertThat(updateAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createRsvp_withBlankGuestName_returns400BadRequest() {
    RsvpRequest invalidRequest = new RsvpRequest();
    invalidRequest.setGuestName("");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps",
            HttpMethod.POST,
            new HttpEntity<>(invalidRequest, authHeaders()),
            String.class,
            ownedEventId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void createRsvp_forNonExistentEvent_returns404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/rsvps",
            HttpMethod.POST,
            new HttpEntity<>(buildRsvpRequest("Nobody", null), authHeaders()),
            String.class,
            987654321L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createRsvp_withoutAuthentication_isRejected() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/events/" + ownedEventId + "/rsvps",
            new HttpEntity<>(buildRsvpRequest("No Auth", null)),
            String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }
}
