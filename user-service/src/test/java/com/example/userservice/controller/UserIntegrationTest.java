package com.example.userservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Full-stack integration tests for the user-service HTTP API. These tests talk to the real,
 * already-running Spring context (random port) and the real shared Postgres database configured in
 * application.properties - no mocks, no H2, no Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private String email;
  private final String password = "SecurePass123";
  private final String fullName = "Integration Test User";

  private Long registeredUserId;
  private String accessToken;

  @BeforeAll
  void generateUniqueEmail() {
    email = "test-" + UUID.randomUUID() + "@example.com";
  }

  @AfterAll
  void cleanUp() {
    // Deactivate (not seed data) the user we created, using its own token, so we never
    // leave test users lying around as active accounts.
    if (accessToken != null) {
      HttpHeaders headers = bearerHeaders(accessToken);
      restTemplate.exchange(
          "/api/v1/users/me", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
  }

  private HttpHeaders bearerHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private RegisterRequest buildRegisterRequest() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName(fullName);
    request.setEmail(email);
    request.setPassword(password);
    request.setPhoneNumber("+15550100");
    return request;
  }

  @Test
  @Order(1)
  void register_withNewUniqueEmail_returns201AndCreatesUser() {
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity(
            "/api/v1/auth/register", buildRegisterRequest(), AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getAccessToken()).isNotBlank();
    assertThat(body.getUser()).isNotNull();
    assertThat(body.getUser().getId()).isNotNull();
    assertThat(body.getUser().getEmail()).isEqualTo(email);
    assertThat(body.getUser().getFullName()).isEqualTo(fullName);
    // Normally CUSTOMER, but if this shared database has no ADMIN yet the bootstrap rule in
    // AuthService promotes this first registration to ADMIN. Both are correct depending on the
    // state of the database; AuthServiceTest pins down each branch deterministically.
    assertThat(body.getUser().getRole()).isIn(Role.CUSTOMER, Role.ADMIN);
    assertThat(body.getUser().isActive()).isTrue();

    registeredUserId = body.getUser().getId();
  }

  @Test
  @Order(2)
  void register_withDuplicateEmail_returns409Conflict() {
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/register", buildRegisterRequest(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains(email);
  }

  @Test
  @Order(3)
  void login_withWrongPassword_returns401() {
    LoginRequest request = new LoginRequest();
    request.setEmail(email);
    request.setPassword("totallyWrongPassword");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/login", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @Order(4)
  void login_withCorrectCredentials_returns200AndToken() {
    LoginRequest request = new LoginRequest();
    request.setEmail(email);
    request.setPassword(password);

    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/api/v1/auth/login", request, AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getAccessToken()).isNotBlank();
    assertThat(body.getUser().getEmail()).isEqualTo(email);

    accessToken = body.getAccessToken();
  }

  @Test
  @Order(5)
  void getCurrentUser_withValidToken_returns200AndCorrectProfile() {
    ResponseEntity<UserResponse> response =
        restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(accessToken)),
            UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    UserResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getId()).isEqualTo(registeredUserId);
    assertThat(body.getEmail()).isEqualTo(email);
    assertThat(body.getFullName()).isEqualTo(fullName);
    assertThat(body.isActive()).isTrue();
  }

  @Test
  @Order(6)
  void getCurrentUser_withoutToken_returnsUnauthorizedOrForbidden() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }

  @Test
  @Order(7)
  void getCurrentUser_withGarbageToken_returnsUnauthorizedOrForbidden() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders("not-a-real-jwt")),
            String.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }

  @Test
  @Order(8)
  void updateCurrentUser_withValidToken_updatesFullNameAndPhoneNumber() {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("Updated Integration User");
    request.setPhoneNumber("+15550199");

    ResponseEntity<UserResponse> response =
        restTemplate.exchange(
            "/api/v1/users/me",
            HttpMethod.PUT,
            new HttpEntity<>(request, bearerHeaders(accessToken)),
            UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    UserResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getId()).isEqualTo(registeredUserId);
    assertThat(body.getFullName()).isEqualTo("Updated Integration User");
    assertThat(body.getPhoneNumber()).isEqualTo("+15550199");
    assertThat(body.getEmail()).isEqualTo(email);
  }

  @Test
  @Order(9)
  void getById_withValidTokenAndOwnId_returnsSameProfile() {
    ResponseEntity<UserResponse> response =
        restTemplate.exchange(
            "/api/v1/users/" + registeredUserId,
            HttpMethod.GET,
            new HttpEntity<>(bearerHeaders(accessToken)),
            UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getEmail()).isEqualTo(email);
  }

  @Test
  @Order(10)
  void existsEndpoint_isPubliclyAccessibleWithoutToken() {
    ResponseEntity<Boolean> response =
        restTemplate.getForEntity("/api/v1/users/" + registeredUserId + "/exists", Boolean.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isTrue();
  }
}
