package com.example.gatewayservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

// Auto-maintained by create_microservice — regenerated with the full service list
// every time a new microservice is created. Do NOT hand-edit, replace, or
// "simplify" this file: it exists so gateway routing is proven with a real HTTP
// call instead of an agent-authored placeholder assertion.
class GatewayRoutingTest {

  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static final int GATEWAY_PORT = 27193;

  private void assertRoute(String service) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create("http://localhost:" + GATEWAY_PORT + "/" + service + "/actuator/health"))
            .GET()
            .build();
    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), "gateway did not route to " + service);
    assertTrue(
        response.body().contains("UP"),
        "unexpected health body from " + service + ": " + response.body());
  }

  @Test
  void routesToHallService() throws Exception {
    assertRoute("hall-service");
  }

  @Test
  void routesToVendorService() throws Exception {
    assertRoute("vendor-service");
  }

  @Test
  void routesToUserService() throws Exception {
    assertRoute("user-service");
  }

  @Test
  void routesToProposalService() throws Exception {
    assertRoute("proposal-service");
  }

  @Test
  void routesToEventService() throws Exception {
    assertRoute("event-service");
  }
}
