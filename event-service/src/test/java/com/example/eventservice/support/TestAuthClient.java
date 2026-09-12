package com.example.eventservice.support;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches a real JWT from the already-running user-service (http://localhost:24800) so
 * event-service integration tests can exercise protected endpoints with a genuine token.
 *
 * <p>This deliberately uses a plain {@link RestTemplate} (not the event-service TestRestTemplate)
 * since user-service is a separate, independently running Spring Boot process.
 */
public final class TestAuthClient {

  private static final String USER_SERVICE_BASE_URL = "http://localhost:24800";
  private static final String TEST_PASSWORD = "SecurePass123";

  private TestAuthClient() {}

  public static AuthResult registerAndLogin() {
    RestTemplate restTemplate = new RestTemplate();
    String email = "test-" + UUID.randomUUID() + "@example.com";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> registerBody = new HashMap<>();
    registerBody.put("fullName", "Event Service Integration Test User");
    registerBody.put("email", email);
    registerBody.put("password", TEST_PASSWORD);
    registerBody.put("phoneNumber", "+15550100");

    ResponseEntity<Map> registerResponse =
        restTemplate.postForEntity(
            USER_SERVICE_BASE_URL + "/api/v1/auth/register",
            new HttpEntity<>(registerBody, headers),
            Map.class);

    if (registerResponse.getStatusCode() != HttpStatus.CREATED
        || registerResponse.getBody() == null) {
      throw new IllegalStateException(
          "Failed to register test user against user-service: " + registerResponse);
    }

    Map<String, Object> loginBody = new HashMap<>();
    loginBody.put("email", email);
    loginBody.put("password", TEST_PASSWORD);

    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            USER_SERVICE_BASE_URL + "/api/v1/auth/login",
            new HttpEntity<>(loginBody, headers),
            Map.class);

    if (loginResponse.getStatusCode() != HttpStatus.OK || loginResponse.getBody() == null) {
      throw new IllegalStateException(
          "Failed to login test user against user-service: " + loginResponse);
    }

    String accessToken = (String) loginResponse.getBody().get("accessToken");
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalStateException(
          "user-service login response did not contain an accessToken: " + loginResponse.getBody());
    }

    Object userObj = loginResponse.getBody().get("user");
    Long userId = null;
    if (userObj instanceof Map<?, ?> userMap) {
      Object idValue = userMap.get("id");
      if (idValue instanceof Number number) {
        userId = number.longValue();
      }
    }

    return new AuthResult(accessToken, userId, email);
  }

  public static final class AuthResult {
    public final String accessToken;
    public final Long userId;
    public final String email;

    public AuthResult(String accessToken, Long userId, String email) {
      this.accessToken = accessToken;
      this.userId = userId;
      this.email = email;
    }
  }
}
