package com.example.proposalservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.proposalservice.support.TestAuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Real, end-to-end integration test for proposal-service, exercising its actual cross-service
 * RestTemplate calls (via Eureka discovery) to the already-running hall-service, vendor-service and
 * event-service. No mocking of those callees.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProposalIntegrationTest {

  private static final String HALL_SERVICE_BASE_URL = "http://localhost:26444";
  private static final String VENDOR_SERVICE_BASE_URL = "http://localhost:25485";
  private static final String EVENT_SERVICE_BASE_URL = "http://localhost:23021";
  private static final Long SEEDED_HALL_ID = 1L;
  private static final Long SEEDED_VENDOR_ID = 1L;

  @LocalServerPort private int port;

  private static TestAuthHelper.AuthenticatedUser authenticatedUser;

  private final RestTemplate plainRestTemplate = new RestTemplate();
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  private Long createdProposalId;

  @BeforeAll
  static void obtainRealToken() {
    authenticatedUser = TestAuthHelper.registerAndLogin();
  }

  @AfterEach
  void cleanup() {
    if (createdProposalId != null) {
      try {
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            Void.class);
      } catch (Exception ignored) {
        // best-effort cleanup
      }
      createdProposalId = null;
    }
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  private HttpHeaders authHeaders() {
    return TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken());
  }

  @Test
  @Order(1)
  void fullProposalLifecycle_create_addItem_applyCoupon_share_book_verifiesRealDownstreamCalls() {
    // ---- Fetch the real hall directly from hall-service, to compare against later ----
    ResponseEntity<Map> hallResponse =
        plainRestTemplate.exchange(
            HALL_SERVICE_BASE_URL + "/api/v1/halls/" + SEEDED_HALL_ID,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Map.class);
    assertThat(hallResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    BigDecimal realHallBasePrice =
        new BigDecimal(hallResponse.getBody().get("basePrice").toString());

    // ---- 1. Create a draft proposal referencing the real seeded hall id=1 ----
    Map<String, Object> createBody = new HashMap<>();
    createBody.put("userId", authenticatedUser.getUserId());
    createBody.put("hallId", SEEDED_HALL_ID);

    ResponseEntity<Map> createResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals",
            HttpMethod.POST,
            new HttpEntity<>(createBody, authHeaders()),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> createdProposal = createResponse.getBody();
    assertThat(createdProposal).isNotNull();
    createdProposalId = ((Number) createdProposal.get("id")).longValue();
    BigDecimal returnedHallBasePrice =
        new BigDecimal(createdProposal.get("hallBasePrice").toString());

    // Proves the real cross-service call to hall-service happened and its result was used
    assertThat(returnedHallBasePrice).isEqualByComparingTo(realHallBasePrice);
    assertThat(createdProposal.get("status")).isEqualTo("DRAFT");

    // ---- 2. Add an item referencing the real seeded vendor id=1 ----
    Map<String, Object> itemBody = new HashMap<>();
    itemBody.put("vendorId", SEEDED_VENDOR_ID);
    itemBody.put("serviceType", "PHOTOGRAPHY");
    itemBody.put("price", 45000.00);

    ResponseEntity<Map> addItemResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId + "/items",
            HttpMethod.POST,
            new HttpEntity<>(itemBody, authHeaders()),
            Map.class);
    assertThat(addItemResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> afterItem = addItemResponse.getBody();
    assertThat(afterItem).isNotNull();
    BigDecimal totalAfterItem = new BigDecimal(afterItem.get("estimatedTotal").toString());
    // total must reflect at least hall base price + item price (seasonal multiplier only increases
    // it further)
    assertThat(totalAfterItem).isGreaterThan(realHallBasePrice);

    // ---- 3. Apply a real seeded coupon code (WELCOME10, from proposal-service's own data.sql)
    // ----
    Map<String, Object> couponBody = new HashMap<>();
    couponBody.put("code", "WELCOME10");

    ResponseEntity<Map> couponResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId + "/coupon",
            HttpMethod.POST,
            new HttpEntity<>(couponBody, authHeaders()),
            Map.class);
    assertThat(couponResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> afterCoupon = couponResponse.getBody();
    assertThat(afterCoupon).isNotNull();
    assertThat(afterCoupon.get("couponCode")).isEqualTo("WELCOME10");
    BigDecimal totalAfterCoupon = new BigDecimal(afterCoupon.get("estimatedTotal").toString());
    // A 10% discount should reduce the total relative to the pre-discount total
    assertThat(totalAfterCoupon).isLessThan(totalAfterItem);
    assertThat(totalAfterCoupon).isGreaterThan(BigDecimal.ZERO);

    // ---- 4. Share the proposal ----
    ResponseEntity<Map> shareResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId + "/share",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders()),
            Map.class);
    assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(shareResponse.getBody().get("status")).isEqualTo("SHARED");

    // ---- 5. Create a fresh, unique hall slot directly against hall-service to book against ----
    long slotId = createFreshSlotForHall(SEEDED_HALL_ID);

    // ---- 6. Book the proposal: triggers hall-service booking + vendor-service commission(s) +
    // event-service event creation ----
    Map<String, Object> bookBody = new HashMap<>();
    bookBody.put("slotId", slotId);
    bookBody.put("guestCount", 250);

    ResponseEntity<Map> bookResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId + "/book",
            HttpMethod.POST,
            new HttpEntity<>(bookBody, authHeaders()),
            Map.class);
    assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> bookResult = bookResponse.getBody();
    assertThat(bookResult).isNotNull();

    Object bookingIdRaw = bookResult.get("bookingId");
    Object eventIdRaw = bookResult.get("eventId");
    assertThat(bookingIdRaw)
        .as("bookingId must be non-null: proves hall-service booking call succeeded")
        .isNotNull();
    assertThat(eventIdRaw)
        .as("eventId must be non-null: proves event-service event-creation call succeeded")
        .isNotNull();

    long bookingId = ((Number) bookingIdRaw).longValue();
    long eventId = ((Number) eventIdRaw).longValue();

    Map<String, Object> confirmedProposal = (Map<String, Object>) bookResult.get("proposal");
    assertThat(confirmedProposal.get("status")).isEqualTo("CONFIRMED");

    // ---- 7. Verify directly against vendor-service that a commission record referencing this
    // bookingId now exists ----
    ResponseEntity<Map> commissionsResponse =
        plainRestTemplate.exchange(
            VENDOR_SERVICE_BASE_URL
                + "/api/v1/vendors/"
                + SEEDED_VENDOR_ID
                + "/commissions?size=100",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Map.class);
    assertThat(commissionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    java.util.List<Map<String, Object>> commissionContent =
        (java.util.List<Map<String, Object>>) commissionsResponse.getBody().get("content");
    boolean hasMatchingCommission =
        commissionContent.stream()
            .anyMatch(c -> ((Number) c.get("bookingId")).longValue() == bookingId);
    assertThat(hasMatchingCommission)
        .as("vendor-service should have received the commission call for bookingId=" + bookingId)
        .isTrue();

    // ---- 8. Verify directly against event-service that the event exists with the correct
    // hallId/userId ----
    ResponseEntity<Map> eventResponse =
        plainRestTemplate.exchange(
            EVENT_SERVICE_BASE_URL + "/api/v1/events/" + eventId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Map.class);
    assertThat(eventResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> eventBody = eventResponse.getBody();
    assertThat(eventBody).isNotNull();
    assertThat(((Number) eventBody.get("hallId")).longValue()).isEqualTo(SEEDED_HALL_ID);
    assertThat(((Number) eventBody.get("userId")).longValue())
        .isEqualTo(authenticatedUser.getUserId());
  }

  @Test
  @Order(2)
  void book_onAlreadyConfirmedProposal_isRejected() {
    // pre-seeded proposal id=1 is DRAFT by default in data.sql; instead we exercise the
    // rule end-to-end by creating+booking a fresh proposal, then attempting to book it again.
    Map<String, Object> createBody = new HashMap<>();
    createBody.put("userId", authenticatedUser.getUserId());
    createBody.put("hallId", SEEDED_HALL_ID);
    ResponseEntity<Map> createResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals",
            HttpMethod.POST,
            new HttpEntity<>(createBody, authHeaders()),
            Map.class);
    createdProposalId = ((Number) createResponse.getBody().get("id")).longValue();

    long slotId = createFreshSlotForHall(SEEDED_HALL_ID);
    Map<String, Object> bookBody = new HashMap<>();
    bookBody.put("slotId", slotId);
    bookBody.put("guestCount", 100);

    ResponseEntity<Map> firstBook =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId + "/book",
            HttpMethod.POST,
            new HttpEntity<>(bookBody, authHeaders()),
            Map.class);
    assertThat(firstBook.getStatusCode()).isEqualTo(HttpStatus.OK);

    long secondSlotId = createFreshSlotForHall(SEEDED_HALL_ID);
    Map<String, Object> secondBookBody = new HashMap<>();
    secondBookBody.put("slotId", secondSlotId);
    secondBookBody.put("guestCount", 100);

    ResponseEntity<Map> secondBook =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/proposals/" + createdProposalId + "/book",
            HttpMethod.POST,
            new HttpEntity<>(secondBookBody, authHeaders()),
            Map.class);
    assertThat(secondBook.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  private long createFreshSlotForHall(Long hallId) {
    Map<String, Object> slotBody = new HashMap<>();
    // Far-future, randomized date to avoid clashing with seeded slots or slots created by other
    // test runs
    LocalDate slotDate =
        LocalDate.now().plusDays(1000 + ThreadLocalRandom.current().nextInt(1, 100000));
    slotBody.put("slotDate", slotDate.toString());
    slotBody.put("slotType", "MORNING");

    ResponseEntity<Map> slotResponse =
        plainRestTemplate.exchange(
            HALL_SERVICE_BASE_URL + "/api/v1/halls/" + hallId + "/slots",
            HttpMethod.POST,
            new HttpEntity<>(slotBody, authHeaders()),
            Map.class);
    assertThat(slotResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return ((Number) slotResponse.getBody().get("id")).longValue();
  }
}
