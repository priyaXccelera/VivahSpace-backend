package com.example.proposalservice.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the caller's Authorization bearer token onto outgoing inter-service RestTemplate calls
 * (hall-service, vendor-service, event-service), since each of those services independently
 * validates the same shared HS256 JWT and rejects unauthenticated requests.
 */
public class AuthHeaderForwardingInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      HttpServletRequest servletRequest = attributes.getRequest();
      String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
      }
    }
    return execution.execute(request, body);
  }
}
