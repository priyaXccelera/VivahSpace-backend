package com.example.vendorservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.vendorservice.dto.PortfolioItemRequest;
import com.example.vendorservice.dto.RatingUpdateRequest;
import com.example.vendorservice.dto.VendorRequest;
import com.example.vendorservice.dto.VerificationUpdateRequest;
import com.example.vendorservice.entity.VendorCategory;
import com.example.vendorservice.entity.VerificationStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * Full integration tests for the vendor-service running as an embedded Spring Boot application
 * against the real Postgres database configured in application.properties.
 *
 * <p>Auth tokens are obtained from the already-running user-service (http://localhost:24800).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VendorIntegrationTest {

  private static final String USER_SERVICE_BASE_URL = "http://localhost:24800";

  @Autowired private TestRestTemplate restTemplate;

  private static String jwtToken;

  // ids created during tests that must be cleaned up
  private static final List<Long> createdVendorIds = new CopyOnWriteArrayList<>();

  @BeforeAll
  static void obtainJwtToken() {
    RestTemplate userServiceClient = new RestTemplate();

    String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
    String password = "TestPass123!";

    Map<String, Object> registerBody =
        Map.of(
            "fullName",
            "Vendor Integration Tester",
            "email",
            uniqueEmail,
            "password",
            password,
            "phoneNumber",
            "9998887777");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

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

    jwtToken = (String) loginResponse.getBody().get("accessToken");
    assertThat(jwtToken).isNotBlank();
  }

  private HttpHeaders authJsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(jwtToken);
    return headers;
  }

  private VendorRequest buildVendorRequest(String suffix) {
    VendorRequest request = new VendorRequest();
    request.setOwnerName("Test Owner " + suffix);
    request.setBusinessName("Test Business " + suffix);
    request.setCategory(VendorCategory.PHOTOGRAPHY);
    request.setLocation("Test City");
    request.setDescription("Integration test vendor");
    request.setBasePrice(new BigDecimal("1000.00"));
    request.setContactEmail("vendor-" + suffix + "@example.com");
    request.setContactPhone("9000000000");
    return request;
  }

  private ResponseEntity<Map<String, Object>> createVendor(VendorRequest request) {
    return restTemplate.exchange(
        "/api/v1/vendors",
        HttpMethod.POST,
        new HttpEntity<>(request, authJsonHeaders()),
        new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  private Long trackAndGetId(Map<String, Object> body) {
    Long id = ((Number) body.get("id")).longValue();
    createdVendorIds.add(id);
    return id;
  }

  private void deleteVendorQuietly(Long id) {
    try {
      restTemplate.exchange(
          "/api/v1/vendors/" + id,
          HttpMethod.DELETE,
          new HttpEntity<>(authJsonHeaders()),
          Void.class);
    } catch (Exception e) {
      System.err.println("Cleanup: failed to delete vendor " + id + ": " + e.getMessage());
    } finally {
      createdVendorIds.remove(id);
    }
  }

  @Test
  void fullVendorCrudLifecycle_createListGetUpdateDeleteThenNotFound() {
    // CREATE
    VendorRequest createRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    ResponseEntity<Map<String, Object>> createResponse = createVendor(createRequest);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> createdBody = createResponse.getBody();
    assertThat(createdBody).isNotNull();
    Long vendorId = trackAndGetId(createdBody);
    assertThat(createdBody.get("businessName")).isEqualTo(createRequest.getBusinessName());
    assertThat(createdBody.get("verificationStatus")).isEqualTo("UNVERIFIED");

    // LIST
    ResponseEntity<Map<String, Object>> listResponse =
        restTemplate.exchange(
            "/api/v1/vendors?size=200",
            HttpMethod.GET,
            new HttpEntity<>(authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) listResponse.getBody().get("content");
    boolean found = content.stream().anyMatch(v -> ((Number) v.get("id")).longValue() == vendorId);
    assertThat(found).isTrue();

    // GET BY ID
    ResponseEntity<Map<String, Object>> getResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId,
            HttpMethod.GET,
            new HttpEntity<>(authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().get("businessName"))
        .isEqualTo(createRequest.getBusinessName());

    // UPDATE
    VendorRequest updateRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    updateRequest.setBusinessName("Updated Business Name");
    updateRequest.setBasePrice(new BigDecimal("2500.50"));
    ResponseEntity<Map<String, Object>> updateResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody().get("businessName")).isEqualTo("Updated Business Name");
    assertThat(new BigDecimal(updateResponse.getBody().get("basePrice").toString()))
        .isEqualByComparingTo("2500.50");

    // DELETE
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId,
            HttpMethod.DELETE,
            new HttpEntity<>(authJsonHeaders()),
            Void.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    createdVendorIds.remove(vendorId);

    // GET BY ID AGAIN -> 404
    ResponseEntity<Map<String, Object>> afterDeleteResponse =
        restTemplate.exchange(
            "/api/v1/vendors/" + vendorId,
            HttpMethod.GET,
            new HttpEntity<>(authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    assertThat(afterDeleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void unauthenticatedRequest_toProtectedEndpoint_isRejected() {
    // Use a known seeded vendor id (1) purely to hit a protected endpoint; we never modify it.
    ResponseEntity<String> response =
        restTemplate.exchange("/api/v1/vendors/1", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    // With Spring Security's default anonymous authentication enabled, a request with no
    // credentials reaching an endpoint that requires authentication is treated as an
    // access-denied case (403), not a missing-authentication case (401), since the request
    // is still associated with an (anonymous) authenticated principal.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void createVendor_withInvalidPayload_returnsBadRequest() {
    VendorRequest invalidRequest = new VendorRequest();
    // missing all required fields (ownerName, businessName, category, location, basePrice,
    // contactEmail, contactPhone)

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors",
            HttpMethod.POST,
            new HttpEntity<>(invalidRequest, authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getVendorById_notFound_returns404() {
    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            "/api/v1/vendors/999999999",
            HttpMethod.GET,
            new HttpEntity<>(authJsonHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().get("message").toString()).contains("999999999");
  }

  @Test
  void updateVerificationStatus_transitionsToVerified() {
    VendorRequest createRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    ResponseEntity<Map<String, Object>> createResponse = createVendor(createRequest);
    Long vendorId = trackAndGetId(createResponse.getBody());

    try {
      VerificationUpdateRequest verificationRequest = new VerificationUpdateRequest();
      verificationRequest.setStatus(VerificationStatus.VERIFIED);

      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/verification",
              HttpMethod.PATCH,
              new HttpEntity<>(verificationRequest, authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().get("verificationStatus")).isEqualTo("VERIFIED");
    } finally {
      deleteVendorQuietly(vendorId);
    }
  }

  @Test
  void updateRating_withinBounds_updatesSuccessfully() {
    VendorRequest createRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    ResponseEntity<Map<String, Object>> createResponse = createVendor(createRequest);
    Long vendorId = trackAndGetId(createResponse.getBody());

    try {
      RatingUpdateRequest ratingRequest = new RatingUpdateRequest();
      ratingRequest.setRating(new BigDecimal("4.25"));

      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/rating",
              HttpMethod.PATCH,
              new HttpEntity<>(ratingRequest, authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(new BigDecimal(response.getBody().get("rating").toString()))
          .isEqualByComparingTo("4.25");
    } finally {
      deleteVendorQuietly(vendorId);
    }
  }

  @Test
  void updateRating_aboveMax_returnsBadRequest() {
    VendorRequest createRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    ResponseEntity<Map<String, Object>> createResponse = createVendor(createRequest);
    Long vendorId = trackAndGetId(createResponse.getBody());

    try {
      RatingUpdateRequest ratingRequest = new RatingUpdateRequest();
      ratingRequest.setRating(new BigDecimal("5.50")); // above @DecimalMax("5.0")

      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/rating",
              HttpMethod.PATCH,
              new HttpEntity<>(ratingRequest, authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    } finally {
      deleteVendorQuietly(vendorId);
    }
  }

  @Test
  void updateRating_belowMin_returnsBadRequest() {
    VendorRequest createRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    ResponseEntity<Map<String, Object>> createResponse = createVendor(createRequest);
    Long vendorId = trackAndGetId(createResponse.getBody());

    try {
      RatingUpdateRequest ratingRequest = new RatingUpdateRequest();
      ratingRequest.setRating(new BigDecimal("-1.0")); // below @DecimalMin("0.0")

      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/rating",
              HttpMethod.PATCH,
              new HttpEntity<>(ratingRequest, authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    } finally {
      deleteVendorQuietly(vendorId);
    }
  }

  @Test
  void portfolioLifecycle_addListDelete() {
    VendorRequest createRequest = buildVendorRequest(UUID.randomUUID().toString().substring(0, 8));
    ResponseEntity<Map<String, Object>> createResponse = createVendor(createRequest);
    Long vendorId = trackAndGetId(createResponse.getBody());

    try {
      PortfolioItemRequest itemRequest = new PortfolioItemRequest();
      itemRequest.setTitle("Integration Test Portfolio Item");
      itemRequest.setImageUrl("https://example.com/test.jpg");
      itemRequest.setDescription("Created by integration test");

      ResponseEntity<Map<String, Object>> addResponse =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/portfolio",
              HttpMethod.POST,
              new HttpEntity<>(itemRequest, authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});
      assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      Long itemId = ((Number) addResponse.getBody().get("id")).longValue();
      assertThat(addResponse.getBody().get("vendorId")).isEqualTo(vendorId.intValue());

      ResponseEntity<Map<String, Object>> listResponse =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/portfolio",
              HttpMethod.GET,
              new HttpEntity<>(authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});
      assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items =
          (List<Map<String, Object>>) listResponse.getBody().get("content");
      assertThat(items).hasSize(1);
      assertThat(items.get(0).get("title")).isEqualTo("Integration Test Portfolio Item");

      ResponseEntity<Void> deleteItemResponse =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/portfolio/" + itemId,
              HttpMethod.DELETE,
              new HttpEntity<>(authJsonHeaders()),
              Void.class);
      assertThat(deleteItemResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

      ResponseEntity<Map<String, Object>> listAfterDelete =
          restTemplate.exchange(
              "/api/v1/vendors/" + vendorId + "/portfolio",
              HttpMethod.GET,
              new HttpEntity<>(authJsonHeaders()),
              new ParameterizedTypeReference<Map<String, Object>>() {});
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> itemsAfterDelete =
          (List<Map<String, Object>>) listAfterDelete.getBody().get("content");
      assertThat(itemsAfterDelete).isEmpty();
    } finally {
      deleteVendorQuietly(vendorId);
    }
  }
}
