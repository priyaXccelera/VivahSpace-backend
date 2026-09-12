package com.example.vendorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class VendorServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(VendorServiceApplication.class, args);
  }
}

// PLACEHOLDER — replace with real entity/DTO/security/controller classes per
// this service's business scope. Keep the /vendors health-style route
// or replace it once real endpoints exist.
@RestController
class VendorServicePlaceholderController {

  @GetMapping("/vendors")
  public String placeholder() {
    return "vendor-service is up — replace this with real endpoints";
  }
}
