package com.example.vendorservice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI vendorServiceOpenAPI() {
    return new OpenAPI().addServersItem(new Server().url("/vendor-service"));
  }
}
