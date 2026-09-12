package com.example.eventservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventservice.dto.ChecklistItemRequest;
import com.example.eventservice.dto.ChecklistItemResponse;
import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.support.TestAuthClient;
import java.time.LocalDate;
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
 * Full-stack integration tests for the checklist sub-resource of an event
 * (/api/v1/events/{eventId}/checklist). Runs against the real, already-running event-service Spring
 * context and the real shared Postgres database - no mocks, no H2, no Testcontainers.
 *
 * <p>A dedicated event is created by this test class for checklist manipulation and torn down
 * afterwards; the seeded demo event/checklist rows (ids 1-2 from data.sql) are never touched.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChecklistIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private String accessToken;
  private Long ownedEventId;
  private final List<Long> createdItemIds = new ArrayList<>();

  @BeforeAll
  void setUp() {
    TestAuthClient.AuthResult authResult = TestAuthClient.registerAndLogin();
    this.accessToken = authResult.accessToken;
    Long userId = authResult.userId != null ? authResult.userId : 123456789L;

    EventRequest eventRequest = new EventRequest();
    eventRequest.setProposalId(9201L);
    eventRequest.setHallId(9202L);
    eventRequest.setUserId(userId);
    eventRequest.setGuestCount(60);

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
  void cleanupRemainingItems() {
    for (Long id : new ArrayList<>(createdItemIds)) {
      restTemplate.exchange(
          "/api/v1/events/{eventId}/checklist/{itemId}",
          HttpMethod.DELETE,
          new HttpEntity<>(authHeaders()),
          Void.class,
          ownedEventId,
          id);
    }
    createdItemIds.clear();
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

  private ChecklistItemRequest buildRequest(String title, Boolean done, LocalDate dueDate) {
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle(title);
    request.setDone(done);
    request.setDueDate(dueDate);
    return request;
  }

  @Test
  void createListUpdateDeleteChecklistItem_worksEndToEnd() {
    // CREATE (done omitted -> defaults false)
    ResponseEntity<ChecklistItemResponse> createResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist",
            HttpMethod.POST,
            new HttpEntity<>(
                buildRequest("Book photographer", null, LocalDate.now().plusDays(10)),
                authHeaders()),
            ChecklistItemResponse.class,
            ownedEventId);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ChecklistItemResponse created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.getId()).isNotNull();
    assertThat(created.getEventId()).isEqualTo(ownedEventId);
    assertThat(created.getTitle()).isEqualTo("Book photographer");
    assertThat(created.isDone()).isFalse();

    Long itemId = created.getId();
    createdItemIds.add(itemId);

    // LIST
    ResponseEntity<String> listResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class,
            ownedEventId);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).contains("\"id\":" + itemId);

    // UPDATE - toggle done flag to true
    ResponseEntity<ChecklistItemResponse> updateResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist/{itemId}",
            HttpMethod.PUT,
            new HttpEntity<>(
                buildRequest("Book photographer", true, LocalDate.now().plusDays(10)),
                authHeaders()),
            ChecklistItemResponse.class,
            ownedEventId,
            itemId);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody()).isNotNull();
    assertThat(updateResponse.getBody().isDone()).isTrue();

    // DELETE
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist/{itemId}",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            Void.class,
            ownedEventId,
            itemId);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    createdItemIds.remove(itemId);

    // Subsequent update on deleted item -> 404
    ResponseEntity<String> updateAfterDelete =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist/{itemId}",
            HttpMethod.PUT,
            new HttpEntity<>(buildRequest("Ghost item", true, null), authHeaders()),
            String.class,
            ownedEventId,
            itemId);
    assertThat(updateAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createChecklistItem_withBlankTitle_returns400BadRequest() {
    ChecklistItemRequest invalidRequest = buildRequest("", null, null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist",
            HttpMethod.POST,
            new HttpEntity<>(invalidRequest, authHeaders()),
            String.class,
            ownedEventId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void createChecklistItem_forNonExistentEvent_returns404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/events/{eventId}/checklist",
            HttpMethod.POST,
            new HttpEntity<>(buildRequest("Orphan item", null, null), authHeaders()),
            String.class,
            987654321L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void createChecklistItem_withoutAuthentication_isRejected() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/events/" + ownedEventId + "/checklist",
            new HttpEntity<>(buildRequest("No Auth", null, null)),
            String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }
}
