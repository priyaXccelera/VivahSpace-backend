package com.example.proposalservice.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Registers and logs in a brand-new random user directly against the already-running user-service
 * (http://localhost:24800), returning a real JWT that can be attached as a Bearer token to requests
 * made against proposal-service (and, transitively, to the downstream hall/vendor/event service
 * calls it forwards the header to).
 */
public final class TestAuthHelper {

  public static final String USER_SERVICE_BASE_URL = "http://localhost:24800";

  private TestAuthHelper() {}

  public static AuthenticatedUser registerAndLogin() {
    RestTemplate restTemplate = new RestTemplate();
    String email = "test-" + UUID.randomUUID() + "@example.com";
    String password = "Password123!";

    Map<String, Object> registerBody = new HashMap<>();
    registerBody.put("fullName", "Integration Test User");
    registerBody.put("email", email);
    registerBody.put("password", password);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    restTemplate.postForEntity(
        USER_SERVICE_BASE_URL + "/api/v1/auth/register",
        new HttpEntity<>(registerBody, headers),
        AuthResponse.class);

    Map<String, Object> loginBody = new HashMap<>();
    loginBody.put("email", email);
    loginBody.put("password", password);

    AuthResponse loginResponse =
        restTemplate
            .postForEntity(
                USER_SERVICE_BASE_URL + "/api/v1/auth/login",
                new HttpEntity<>(loginBody, headers),
                AuthResponse.class)
            .getBody();

    if (loginResponse == null || loginResponse.getAccessToken() == null) {
      throw new IllegalStateException("Failed to obtain access token from user-service login");
    }

    Long userId = loginResponse.getUser() != null ? loginResponse.getUser().getId() : null;
    return new AuthenticatedUser(loginResponse.getAccessToken(), userId, email);
  }

  public static HttpHeaders bearerHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    return headers;
  }

  public static class AuthenticatedUser {
    private final String accessToken;
    private final Long userId;
    private final String email;

    public AuthenticatedUser(String accessToken, Long userId, String email) {
      this.accessToken = accessToken;
      this.userId = userId;
      this.email = email;
    }

    public String getAccessToken() {
      return accessToken;
    }

    public Long getUserId() {
      return userId;
    }

    public String getEmail() {
      return email;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AuthResponse {
    private String accessToken;
    private long expiresInMs;
    private AuthUser user;

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public long getExpiresInMs() {
      return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
      this.expiresInMs = expiresInMs;
    }

    public AuthUser getUser() {
      return user;
    }

    public void setUser(AuthUser user) {
      this.user = user;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AuthUser {
    private Long id;
    private String email;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
