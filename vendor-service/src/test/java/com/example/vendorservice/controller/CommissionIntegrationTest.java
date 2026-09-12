package com.example.vendorservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.vendorservice.dto.CommissionRequest;
import com.example.vendorservice.dto.RatingUpdateRequest;
import com.example.vendorservice.dto.VendorRequest;
import com.example.vendorservice.entity.VendorCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for the commission recording / listing / analytics endpoints, run against the
 * embedded vendor-service instance and the real Postgres database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommissionIntegrationTest {

  private static final String USER_SERVICE_BASE_URL = "http://localhost:24800";

  @Autowired private TestRestTemplate restTemplate;

  private String jwtToken;
  private Long vendorId;

  @BeforeAll
  void setUp() {
    jwtToken = obtainJwtToken();
    vendorId = createTestVendor();
  }

  @AfterAll
  void tearDown() {
    if (vendorId != null) {
      try {
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId,
            HttpMethod.DELETE,
            new HttpEntity<>(authJsonHeaders()),
            Void.class);
      } catch (Exception e) {
        // The vendor may still have commission rows referencing it (the commission
        // entity has no cascade-delete and there is no delete-commission endpoint),
        // so deletion can fail with a FK violation. Best-effort cleanup only.
        System.err.println(
            "Cleanup: failed to delete commission-test vendor " + vendorId + ": " + e.getMessage());
      }
    }
  }

  private String obtainJwtToken() {
    RestTemplate userServiceClient = new RestTemplate();
    String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
    String password = "TestPass123!";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> registerBody =
        Map.of(
            "fullName",
            "Commission Integration Tester",
            "email",
            uniqueEmail,
            "password",
            password,
            "phoneNumber",
            "9998887766");
    ResponseEntity<Map<String, Object>> registerResponse =
        userServiceClient.exchange(
            USER_SERVICE_BASE_URL + "/api/v1/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(registerBody, headers),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Map<String, Object> loginBody = Map.of("email", uniqueEmail, "password", password);
    ResponseEntity<Map<String, Object>> loginResponse =
        userServiceClient.exchange(
            USER_SERVICE_BASE_URL + "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(loginBody, headers),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String token = (String) loginResponse.getBody().get("accessToken");
    assertThat(token).isNotBlank();
    return token;
  }

  private HttpHeaders authJsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(jwtToken);
    return headers;
  }

  private Long createTestVendor() {
    VendorRequest request = new VendorRequest();
    request.setOwnerName("Commission Test Owner");
    request.setBusinessName(
        "Commission Test Business " + UUID.randomUUID().toString().substring(0, 8));
    request.setCategory(VendorCategory.CATERING);
    request.setLocation("Test City");
    request.setDescription("Vendor created for commission integration tests");
    request.setBasePrice(new BigDecimal("500.00"));
    request.setContactEmail(
        "commission-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
    request.setContactPhone("9000000001");

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors",
            HttpMethod.POST,
            new HttpEntity<>(request, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return ((Number) response.getBody().get("id")).longValue();
  }

  @Test
  void recordCommission_forExistingVendor_returnsCreatedWithCorrectData() {
    CommissionRequest request = new CommissionRequest();
    request.setBookingId(555001L);
    request.setAmount(new BigDecimal("1234.56"));

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/commissions",
            HttpMethod.POST,
            new HttpEntity<>(request, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> body = response.getBody();
    assertThat(body.get("vendorId")).isEqualTo(vendorId.intValue());
    assertThat(body.get("bookingId")).isEqualTo(555001);
    assertThat(new BigDecimal(body.get("amount").toString())).isEqualByComparingTo("1234.56");
  }

  @Test
  void recordCommission_forNonexistentVendor_returns404() {
    CommissionRequest request = new CommissionRequest();
    request.setBookingId(1L);
    request.setAmount(new BigDecimal("100.00"));

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors/999999999/commissions",
            HttpMethod.POST,
            new HttpEntity<>(request, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void recordCommission_withNonPositiveAmount_returnsBadRequest() {
    CommissionRequest request = new CommissionRequest();
    request.setBookingId(2L);
    request.setAmount(new BigDecimal("-50.00"));

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/commissions",
            HttpMethod.POST,
            new HttpEntity<>(request, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void recordCommission_unauthenticated_isRejected() {
    CommissionRequest request = new CommissionRequest();
    request.setBookingId(3L);
    request.setAmount(new BigDecimal("100.00"));

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/commissions",
            HttpMethod.POST,
            new HttpEntity<>(request, new HttpHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    // With Spring Security's default anonymous authentication enabled, a request with no
    // credentials reaching an endpoint that requires authentication is treated as an
    // access-denied case (403), not a missing-authentication case (401), since the request
    // is still associated with an (anonymous) authenticated principal.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void listAndAnalytics_reflectRecordedCommissions() {
    CommissionRequest first = new CommissionRequest();
    first.setBookingId(700001L);
    first.setAmount(new BigDecimal("300.00"));
    ResponseEntity<Map<String, Object>> firstResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/commissions",
            HttpMethod.POST,
            new HttpEntity<>(first, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    CommissionRequest second = new CommissionRequest();
    second.setBookingId(700002L);
    second.setAmount(new BigDecimal("200.00"));
    ResponseEntity<Map<String, Object>> secondResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/commissions",
            HttpMethod.POST,
            new HttpEntity<>(second, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // set a known rating so analytics.averageRating is deterministic
    RatingUpdateRequest ratingRequest = new RatingUpdateRequest();
    ratingRequest.setRating(new BigDecimal("3.75"));
    restTemplate.exchange(
        "/api/v1/vendors/" + vendorId + "/rating",
        HttpMethod.PATCH,
        new HttpEntity<>(ratingRequest, authJsonHeaders()),
        new ParameterizedTypeReference<Map<String, Object>>() {});

    ResponseEntity<Map<String, Object>> listResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/commissions?size=200",
            HttpMethod.GET,
            new HttpEntity<>(authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listResponse.getBody().get("content");
    long matchingBookingIds =
        content.stream()
            .filter(
                c -> {
                  Object b = c.get("bookingId");
                  long bookingId = ((Number) b).longValue();
                  return bookingId == 700001L || bookingId == 700002L;
                })
            .count();
    assertThat(matchingBookingIds).isEqualTo(2);

    ResponseEntity<Map<String, Object>> analyticsResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId + "/analytics",
            HttpMethod.GET,
            new HttpEntity<>(authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(analyticsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> analyticsBody = analyticsResponse.getBody();
    assertThat(((Number) analyticsBody.get("totalBookings")).longValue())
        .isGreaterThanOrEqualTo(2L);
    assertThat(new BigDecimal(analyticsBody.get("totalCommission").toString()))
        .isGreaterThanOrEqualTo(new BigDecimal("500.00"));
    assertThat(new BigDecimal(analyticsBody.get("averageRating").toString()))
        .isEqualByComparingTo("3.75");
  }
}
