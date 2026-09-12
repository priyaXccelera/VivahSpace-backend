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
 * SeasonalPricingRule endpoints currently only expose create + list (no delete), so this test uses
 * unique, clearly test-scoped rule names and never touches the pre-seeded "Wedding Season Peak"
 * rule (id=1) from proposal-service's data.sql.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeasonalPricingIntegrationTest {

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
    String uniqueName = "IT Off-Season Rule " + UUID.randomUUID().toString().substring(0, 8);

    Map<String, Object> body = new HashMap<>();
    body.put("name", uniqueName);
    body.put("startDate", LocalDate.now().plusYears(5).toString());
    body.put("endDate", LocalDate.now().plusYears(5).plusDays(30).toString());
    body.put("multiplier", new BigDecimal("0.90"));

    ResponseEntity<Map> createResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/seasonal-pricing",
            HttpMethod.POST,
            new HttpEntity<>(
                body, TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.get("name")).isEqualTo(uniqueName);
    assertThat(new BigDecimal(created.get("multiplier").toString())).isEqualByComparingTo("0.90");

    ResponseEntity<Map> listResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/seasonal-pricing?size=200",
            HttpMethod.GET,
            new HttpEntity<>(TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listResponse.getBody().get("content");
    boolean found = content.stream().anyMatch(r -> uniqueName.equals(r.get("name")));
    assertThat(found).isTrue();
  }

  @Test
  void list_isAccessibleAndIncludesSeededRule() {
    ResponseEntity<Map> listResponse =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/seasonal-pricing?size=200",
            HttpMethod.GET,
            new HttpEntity<>(TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listResponse.getBody().get("content");
    boolean hasSeededRule =
        content.stream().anyMatch(r -> "Wedding Season Peak".equals(r.get("name")));
    assertThat(hasSeededRule).as("pre-seeded 'Wedding Season Peak' rule should exist").isTrue();
  }
}
