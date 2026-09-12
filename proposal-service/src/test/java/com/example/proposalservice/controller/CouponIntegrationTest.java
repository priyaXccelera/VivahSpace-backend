package com.example.proposalservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.proposalservice.support.TestAuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Coupons currently only expose create + list endpoints (no delete), so this test only creates
 * coupons with unique, clearly test-scoped codes and never touches the pre-seeded WELCOME10 /
 * FESTIVE20 coupons from proposal-service's data.sql.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponIntegrationTest {

  @LocalServerPort private int port;

  private static TestAuthHelper.AuthenticatedUser authenticatedUser;
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  @BeforeAll
  static void obtainRealToken() {
    authenticatedUser = TestAuthHelper.registerAndLogin();
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  void create_thenListContainsIt() {
    String uniqueCode = "ITCOUPON" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    Map<String, Object> body = new HashMap<>();
    body.put("code", uniqueCode);
    body.put("discountPercent", new BigDecimal("12.50"));
    body.put("expiryDate", LocalDate.now().plusDays(45).toString());
    body.put("active", true);

    ResponseEntity<Map> createResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/coupons",
            HttpMethod.POST,
            new HttpEntity<>(
                body, TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.get("code")).isEqualTo(uniqueCode);
    assertThat(created.get("active")).isEqualTo(true);

    ResponseEntity<Map> listResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/coupons?size=200",
            HttpMethod.GET,
            new HttpEntity<>(TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listResponse.getBody().get("content");
    boolean found = content.stream().anyMatch(c -> uniqueCode.equals(c.get("code")));
    assertThat(found).isTrue();
  }

  @Test
  void create_withDefaultActive_defaultsToTrue() {
    String uniqueCode = "ITDEFAULT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    Map<String, Object> body = new HashMap<>();
    body.put("code", uniqueCode);
    body.put("discountPercent", new BigDecimal("5.00"));
    body.put("expiryDate", LocalDate.now().plusDays(10).toString());

    ResponseEntity<Map> createResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/coupons",
            HttpMethod.POST,
            new HttpEntity<>(
                body, TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse.getBody().get("active")).isEqualTo(true);
  }

  @Test
  void list_isAccessibleAndIncludesSeededCoupons() {
    ResponseEntity<Map> listResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/coupons?size=200",
            HttpMethod.GET,
            new HttpEntity<>(TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listResponse.getBody().get("content");
    boolean hasWelcome10 = content.stream().anyMatch(c -> "WELCOME10".equals(c.get("code")));
    assertThat(hasWelcome10).as("pre-seeded WELCOME10 coupon should exist").isTrue();
  }
}
