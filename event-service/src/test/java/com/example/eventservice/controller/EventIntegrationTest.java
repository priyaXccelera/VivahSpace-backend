package com.example.eventservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.dto.GuestCountUpdateRequest;
import com.example.eventservice.entity.EventStatus;
import com.example.eventservice.support.TestAuthClient;
import java.util.ArrayList;
import java.util.List;
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
 * Full-stack integration tests for the event-service HTTP API. These tests talk to the real,
 * already-running Spring context (random port) and the real shared Postgres database configured in
 * application.properties - no mocks, no H2, no Testcontainers.
 *
 * <p>A real JWT is obtained from the already-running user-service (localhost:24800) via register ->
 * login, and attached as a Bearer token to every authenticated request.
 *
 * <p>Only events created by this test class are mutated/deleted; the seeded demo events (ids 1 and
 * 2 from data.sql) are never touched.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private String accessToken;
  private Long testUserId;

  private final List<Long> createdEventIds = new ArrayList<>();

  @BeforeAll
  void obtainRealJwt() {
    TestAuthClient.AuthResult authResult = TestAuthClient.registerAndLogin();
    this.accessToken = authResult.accessToken;
    this.testUserId = authResult.userId != null ? authResult.userId : 123456789L;
  }

  @AfterEach
  void cleanupAnyRemainingEvents() {
    for (Long id : new ArrayList<>(createdEventIds)) {
      restTemplate.exchange(
          "/api/v1/events/{id}",
          HttpMethod.DELETE,
          new HttpEntity<>(authHeaders()),
          Void.class,
          id);
    }
    createdEventIds.clear();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private EventRequest buildEventRequest(int guestCount) {
    EventRequest request = new EventRequest();
    request.setProposalId(9001L);
    request.setHallId(9002L);
    request.setUserId(testUserId);
    request.setGuestCount(guestCount);
    return request;
  }

  @Test
  void fullCrudLifecycle_createReadUpdateDelete_worksEndToEnd() {
    // CREATE
    ResponseEntity<EventResponse> createResponse =
        restTemplate.exchange(
            "/api/v1/events",
            HttpMethod.POST,
            new HttpEntity<>(buildEventRequest(120), authHeaders()),
            EventResponse.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    EventResponse created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.getId()).isNotNull();
    assertThat(created.getProposalId()).isEqualTo(9001L);
    assertThat(created.getHallId()).isEqualTo(9002L);
    assertThat(created.getUserId()).isEqualTo(testUserId);
    assertThat(created.getGuestCount()).isEqualTo(120);
    assertThat(created.getStatus()).isEqualTo(EventStatus.ACTIVE);

    Long eventId = created.getId();
    createdEventIds.add(eventId);

    // GET list (filtered by our test userId) - our created event should show up
    ResponseEntity<String> listResponse =
        restTemplate.exchange(
            "/api/v1/events?userId=" + testUserId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).contains("\"id\":" + eventId);

    // GET by id
    ResponseEntity<EventResponse> getResponse =
        restTemplate.exchange(
            "/api/v1/events/{id}",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            EventResponse.class,
            eventId);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().getId()).isEqualTo(eventId);
    assertThat(getResponse.getBody().getGuestCount()).isEqualTo(120);

    // PUT guest-count
    GuestCountUpdateRequest guestCountUpdate = new GuestCountUpdateRequest();
    guestCountUpdate.setGuestCount(275);
    ResponseEntity<EventResponse> guestCountResponse =
        restTemplate.exchange(
            "/api/v1/events/{id}/guest-count",
            HttpMethod.PUT,
            new HttpEntity<>(guestCountUpdate, authHeaders()),
            EventResponse.class,
            eventId);
    assertThat(guestCountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(guestCountResponse.getBody()).isNotNull();
    assertThat(guestCountResponse.getBody().getGuestCount()).isEqualTo(275);

    // PUT status
    ResponseEntity<EventResponse> statusResponse =
        restTemplate.exchange(
            "/api/v1/events/{id}/status?status=COMPLETED",
            HttpMethod.PUT,
            new HttpEntity<>(authHeaders()),
            EventResponse.class,
            eventId);
    assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(statusResponse.getBody()).isNotNull();
    assertThat(statusResponse.getBody().getStatus()).isEqualTo(EventStatus.COMPLETED);

    // DELETE
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/api/v1/events/{id}",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            Void.class,
            eventId);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    createdEventIds.remove(eventId);

    // GET by id again -> 404
    ResponseEntity<String> afterDeleteResponse =
        restTemplate.exchange(
            "/api/v1/events/{id}",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class,
            eventId);
    assertThat(afterDeleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createEvent_withInvalidGuestCount_returns400BadRequest() {
    EventRequest invalidRequest = buildEventRequest(0); // @Positive violated

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events",
            HttpMethod.POST,
            new HttpEntity<>(invalidRequest, authHeaders()),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getEvent_nonExistentId_returns404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{id}",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class,
            987654321L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createEvent_withoutAuthentication_isRejected() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/events", new HttpEntity<>(buildEventRequest(50)), String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }

  @Test
  void listEvents_withoutAuthentication_isRejected() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/events", String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }

  @Test
  void seededDemoEvent_isNeverMutatedByThisSuite_sanityReadOnly() {
    // Purely a read of seeded demo data (id 1 from data.sql) to prove we can see it,
    // without ever issuing a PUT/DELETE against it.
    ResponseEntity<EventResponse> response =
        restTemplate.exchange(
            "/api/v1/events/{id}",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            EventResponse.class,
            1L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(1L);
  }
}
