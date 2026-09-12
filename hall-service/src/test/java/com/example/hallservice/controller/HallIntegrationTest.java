package com.example.hallservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Full integration tests for hall-service's Hall CRUD endpoints, running against the service's own
 * embedded servlet container (RANDOM_PORT) and the real Postgres database already configured in
 * application.properties. A real JWT is obtained from the already-running user-service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HallIntegrationTest {

  private static final String USER_SERVICE_BASE_URL = "http://localhost:24800";
  private static final RestTemplate USER_SERVICE_CLIENT = new RestTemplate();

  private static String accessToken;

  @Autowired private TestRestTemplate restTemplate;

  private final List<Long> createdHallIds = new ArrayList<>();

  @BeforeAll
  static void obtainAuthToken() {
    String email = "test-" + UUID.randomUUID() + "@example.com";
    String password = "Password123!";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> registerBody = new HashMap<>();
    registerBody.put("fullName", "Hall Integration Test User");
    registerBody.put("email", email);
    registerBody.put("password", password);

    ResponseEntity<Map> registerResponse =
        USER_SERVICE_CLIENT.postForEntity(
            USER_SERVICE_BASE_URL + "/api/v1/auth/register",
            new HttpEntity<>(registerBody, headers),
            Map.class);
    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

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
        // best-effort cleanup
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

  private Map<String, Object> hallPayload(String name) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("ownerName", "Integration Test Owner");
    payload.put("name", name);
    payload.put("location", "Test City");
    payload.put("description", "A hall created by an integration test");
    payload.put("capacity", 150);
    payload.put("basePrice", 75000.50);
    return payload;
  }

  @Test
  void fullCrudLifecycle_forHall() {
    // CREATE
    String initialName = "IT Hall " + UUID.randomUUID();
    ResponseEntity<Map> createResponse =
        restTemplate.exchange(
            "/api/v1/halls", HttpMethod.POST, authEntity(hallPayload(initialName)), Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> created = createResponse.getBody();
    assertThat(created).isNotNull();
    long hallId = ((Number) created.get("id")).longValue();
    createdHallIds.add(hallId);
    assertThat(created.get("name")).isEqualTo(initialName);
    assertThat(created.get("verificationStatus")).isEqualTo("UNVERIFIED");
    assertThat(((Number) created.get("capacity")).intValue()).isEqualTo(150);

    // LIST (GET all, paginated)
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/halls?size=200", HttpMethod.GET, authEntity(null), Map.class);
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponse.getBody()).containsKey("content");

    // GET by id
    ResponseEntity<Map> getResponse =
        restTemplate.exchange(
            "/api/v1/halls/" + hallId, HttpMethod.GET, authEntity(null), Map.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Number) getResponse.getBody().get("id")).longValue()).isEqualTo(hallId);
    assertThat(getResponse.getBody().get("name")).isEqualTo(initialName);

    // UPDATE
    String updatedName = "Updated IT Hall " + UUID.randomUUID();
    Map<String, Object> updatePayload = hallPayload(updatedName);
    updatePayload.put("capacity", 300);
    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/halls/" + hallId, HttpMethod.PUT, authEntity(updatePayload), Map.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody().get("name")).isEqualTo(updatedName);
    assertThat(((Number) updateResponse.getBody().get("capacity")).intValue()).isEqualTo(300);

    // PATCH verification status
    Map<String, Object> verificationPayload = new HashMap<>();
    verificationPayload.put("status", "VERIFIED");
    ResponseEntity<Map> verificationResponse =
        restTemplate.exchange(
            "/api/v1/halls/" + hallId + "/verification",
            HttpMethod.PATCH,
            authEntity(verificationPayload),
            Map.class);
    assertThat(verificationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(verificationResponse.getBody().get("verificationStatus")).isEqualTo("VERIFIED");

    // DELETE
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/api/v1/halls/" + hallId, HttpMethod.DELETE, authEntity(null), Void.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    createdHallIds.remove(hallId);

    // GET after delete -> 404
    ResponseEntity<Map> afterDeleteResponse =
        restTemplate.exchange(
            "/api/v1/halls/" + hallId, HttpMethod.GET, authEntity(null), Map.class);
    assertThat(afterDeleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void getById_nonExistentHall_returns404() {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/halls/999999999", HttpMethod.GET, authEntity(null), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_invalidPayload_returns400() {
    Map<String, Object> invalidPayload = new HashMap<>();
    invalidPayload.put("ownerName", "");
    invalidPayload.put("name", "");
    invalidPayload.put("location", "");
    invalidPayload.put("capacity", -5);
    invalidPayload.put("basePrice", -10);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/halls", HttpMethod.POST, authEntity(invalidPayload), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_withoutAuthentication_isRejected() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> requestNoAuth =
        new HttpEntity<>(hallPayload("Should Not Be Created"), headers);

    ResponseEntity<Map> response =
        restTemplate.exchange("/api/v1/halls", HttpMethod.POST, requestNoAuth, Map.class);

    assertThat(response.getStatusCode().value()).isIn(401, 403);
  }

  @Test
  void seededDemoHall_isReachableAndUntouched() {
    // Hall id 1 is seeded via data.sql ("Grand Regency Hall") - only read it, never modify/delete
    // it.
    ResponseEntity<Map> response =
        restTemplate.exchange("/api/v1/halls/1", HttpMethod.GET, authEntity(null), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("name")).isEqualTo("Grand Regency Hall");
  }
}
