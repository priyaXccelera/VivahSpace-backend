package com.example.hallservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for booking a hall availability slot and joining a waitlist, running against
 * hall-service's own embedded instance and the real Postgres database. A real JWT is obtained from
 * the already-running user-service.
 *
 * <p>Note: hall-service exposes no DELETE endpoint for bookings/waitlist entries/slots, and
 * cancelling a booking only flips its status (it does not delete the row). Because Booking/Waitlist
 * rows hold a foreign key to the slot/hall they reference, the hall created for this test cannot
 * always be deleted afterwards without violating that FK constraint, so hall cleanup here is
 * best-effort.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingIntegrationTest {

  private static final String USER_SERVICE_BASE_URL = "http://localhost:24800";
  private static final RestTemplate USER_SERVICE_CLIENT = new RestTemplate();

  private static String accessToken;
  private static Long testUserId = 1L;

  @Autowired private TestRestTemplate restTemplate;

  private final List<Long> createdHallIds = new ArrayList<>();

  @BeforeAll
  static void obtainAuthToken() {
    String email = "test-" + UUID.randomUUID() + "@example.com";
    String password = "Password123!";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> registerBody = new HashMap<>();
    registerBody.put("fullName", "Booking Integration Test User");
    registerBody.put("email", email);
    registerBody.put("password", password);

    ResponseEntity<Map> registerResponse =
        USER_SERVICE_CLIENT.postForEntity(
            USER_SERVICE_BASE_URL + "/api/v1/auth/register",
            new HttpEntity<>(registerBody, headers),
            Map.class);
    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> userInfo = (Map<String, Object>) registerResponse.getBody().get("user");
    if (userInfo != null && userInfo.get("id") != null) {
      testUserId = ((Number) userInfo.get("id")).longValue();
    }

    Map<String, Object> loginBody = new HashMap<>();
    loginBody.put("email", email);
    loginBody.put("password", password);

    ResponseEntity<Map> loginResponse =
        USER_SERVICE_CLIENT.postForEntity(
            USER_SERVICE_BASE_URL + "/api/v1/auth/login",
            new HttpEntity<>(loginBody, headers),
            Map.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    accessToken = (String) loginResponse.getBody().get("accessToken");
    assertThat(accessToken).isNotBlank();
  }

  @AfterEach
  void cleanupCreatedHalls() {
    for (Long id : new ArrayList<>(createdHallIds)) {
      try {
        restTemplate.exchange(
            "/api/v1/halls/" + id, HttpMethod.DELETE, authEntity(null), Void.class);
      } catch (Exception ignored) {
        // best-effort: deletion can fail once bookings/waitlist entries reference this hall's slots
      }
    }
    createdHallIds.clear();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private <T> HttpEntity<T> authEntity(T body) {
    return new HttpEntity<>(body, authHeaders());
  }

  private long createHall() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("ownerName", "Booking Test Owner");
    payload.put("name", "Booking Test Hall " + UUID.randomUUID());
    payload.put("location", "Test City");
    payload.put("description", "A hall created for booking integration tests");
    payload.put("capacity", 100);
    payload.put("basePrice", 20000.00);

    ResponseEntity<Map> response =
        restTemplate.exchange("/api/v1/halls", HttpMethod.POST, authEntity(payload), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long id = ((Number) response.getBody().get("id")).longValue();
    createdHallIds.add(id);
    return id;
  }

  private long createSlot(long hallId, LocalDate date, String slotType) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("slotDate", date.toString());
    payload.put("slotType", slotType);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/halls/" + hallId + "/slots", HttpMethod.POST, authEntity(payload), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().get("status")).isEqualTo("AVAILABLE");
    return ((Number) response.getBody().get("id")).longValue();
  }

  @Test
  void bookSlot_thenSlotBecomesUnavailable_andSecondBookingIsRejected() {
    long hallId = createHall();
    long slotId = createSlot(hallId, LocalDate.now().plusDays(30), "MORNING");

    Map<String, Object> bookingPayload = new HashMap<>();
    bookingPayload.put("userId", testUserId);
    bookingPayload.put("proposalId", 42);

    ResponseEntity<Map> bookResponse =
        restTemplate.exchange(
            "/api/v1/slots/" + slotId + "/book",
            HttpMethod.POST,
            authEntity(bookingPayload),
            Map.class);
    assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> booking = bookResponse.getBody();
    assertThat(booking.get("status")).isEqualTo("CONFIRMED");
    assertThat(((Number) booking.get("slotId")).longValue()).isEqualTo(slotId);
    assertThat(((Number) booking.get("hallId")).longValue()).isEqualTo(hallId);
    long bookingId = ((Number) booking.get("id")).longValue();

    // GET the booking back
    ResponseEntity<Map> getBookingResponse =
        restTemplate.exchange(
            "/api/v1/bookings/" + bookingId, HttpMethod.GET, authEntity(null), Map.class);
    assertThat(getBookingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getBookingResponse.getBody().get("status")).isEqualTo("CONFIRMED");

    // Booking the same slot again must be rejected since it's no longer AVAILABLE
    ResponseEntity<Map> secondBookResponse =
        restTemplate.exchange(
            "/api/v1/slots/" + slotId + "/book",
            HttpMethod.POST,
            authEntity(bookingPayload),
            Map.class);
    assertThat(secondBookResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    // List bookings filtered by userId should include this booking
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/bookings?userId=" + testUserId + "&size=200",
            HttpMethod.GET,
            authEntity(null),
            Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Cancel the booking, which should free the slot again
    ResponseEntity<Map> cancelResponse =
        restTemplate.exchange(
            "/api/v1/bookings/" + bookingId + "/cancel",
            HttpMethod.POST,
            authEntity(null),
            Map.class);
    assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelResponse.getBody().get("status")).isEqualTo("CANCELLED");
  }

  @Test
  void joinWaitlist_forABookedSlot_createsUnnotifiedEntry_andCancellingBookingNotifiesIt() {
    long hallId = createHall();
    long slotId = createSlot(hallId, LocalDate.now().plusDays(31), "EVENING");

    // Book the slot first so it becomes unavailable
    Map<String, Object> bookingPayload = new HashMap<>();
    bookingPayload.put("userId", testUserId);
    ResponseEntity<Map> bookResponse =
        restTemplate.exchange(
            "/api/v1/slots/" + slotId + "/book",
            HttpMethod.POST,
            authEntity(bookingPayload),
            Map.class);
    assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long bookingId = ((Number) bookResponse.getBody().get("id")).longValue();

    // A second user joins the waitlist for the now-unavailable slot
    Map<String, Object> waitlistPayload = new HashMap<>();
    long waitlistUserId = testUserId + 999;
    waitlistPayload.put("userId", waitlistUserId);

    ResponseEntity<Map> waitlistResponse =
        restTemplate.exchange(
            "/api/v1/slots/" + slotId + "/waitlist",
            HttpMethod.POST,
            authEntity(waitlistPayload),
            Map.class);
    assertThat(waitlistResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> waitlistEntry = waitlistResponse.getBody();
    assertThat(waitlistEntry.get("notified")).isEqualTo(false);
    assertThat(((Number) waitlistEntry.get("slotId")).longValue()).isEqualTo(slotId);
    assertThat(((Number) waitlistEntry.get("userId")).longValue()).isEqualTo(waitlistUserId);

    // List waitlist entries for this slot
    ResponseEntity<Map> listWaitlistResponse =
        restTemplate.exchange(
            "/api/v1/waitlist?slotId=" + slotId, HttpMethod.GET, authEntity(null), Map.class);
    assertThat(listWaitlistResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Cancelling the booking should notify the first waiting entry
    ResponseEntity<Map> cancelResponse =
        restTemplate.exchange(
            "/api/v1/bookings/" + bookingId + "/cancel",
            HttpMethod.POST,
            authEntity(null),
            Map.class);
    assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelResponse.getBody().get("status")).isEqualTo("CANCELLED");

    ResponseEntity<Map> listAfterCancelResponse =
        restTemplate.exchange(
            "/api/v1/waitlist?slotId=" + slotId, HttpMethod.GET, authEntity(null), Map.class);
    assertThat(listAfterCancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listAfterCancelResponse.getBody().get("content");
    assertThat(content).isNotEmpty();
    boolean anyNotified =
        content.stream().anyMatch(entry -> Boolean.TRUE.equals(entry.get("notified")));
    assertThat(anyNotified).isTrue();
  }

  @Test
  void bookSlot_nonExistentSlot_returns404() {
    Map<String, Object> bookingPayload = new HashMap<>();
    bookingPayload.put("userId", testUserId);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/slots/999999999/book", HttpMethod.POST, authEntity(bookingPayload), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void bookSlot_withoutAuthentication_isRejected() {
    long hallId = createHall();
    long slotId = createSlot(hallId, LocalDate.now().plusDays(32), "FULL_DAY");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> bookingPayload = new HashMap<>();
    bookingPayload.put("userId", testUserId);
    HttpEntity<Map<String, Object>> requestNoAuth = new HttpEntity<>(bookingPayload, headers);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/slots/" + slotId + "/book", HttpMethod.POST, requestNoAuth, Map.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }
}
