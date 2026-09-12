package com.example.proposalservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.proposalservice.support.TestAuthHelper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalyticsIntegrationTest {

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
  void revenue_returns200WithSaneNonNegativeBody() {
    ResponseEntity<Map> response =
        testRestTemplate.exchange(
            baseUrl() + "/api/v1/analytics/revenue",
            HttpMethod.GET,
            new HttpEntity<>(TestAuthHelper.bearerHeaders(authenticatedUser.getAccessToken())),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();

    long confirmedBookings = ((Number) body.get("confirmedBookings")).longValue();
    BigDecimal totalRevenue = new BigDecimal(body.get("totalRevenue").toString());
    BigDecimal hallRevenue = new BigDecimal(body.get("hallRevenue").toString());
    BigDecimal vendorRevenue = new BigDecimal(body.get("vendorRevenue").toString());

    // At least one proposal (the pre-seeded id=1, or one booked in ProposalIntegrationTest)
    // may already be confirmed by the time this test runs; regardless, the fields must be
    // non-negative and internally consistent.
    assertThat(confirmedBookings).isGreaterThanOrEqualTo(0);
    assertThat(totalRevenue).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    assertThat(hallRevenue).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    assertThat(vendorRevenue).isGreaterThanOrEqualTo(BigDecimal.ZERO);

    if (confirmedBookings == 0) {
      assertThat(totalRevenue).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }
}
