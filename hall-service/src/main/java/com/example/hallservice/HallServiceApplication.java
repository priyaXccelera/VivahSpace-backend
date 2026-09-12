package com.example.hallservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class HallServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(HallServiceApplication.class, args);
  }
}

// PLACEHOLDER — replace with real entity/DTO/security/controller classes per
// this service's business scope. Keep the /halls health-style route
// or replace it once real endpoints exist.
@RestController
class HallServicePlaceholderController {

  @GetMapping("/halls")
  public String placeholder() {
    return "hall-service is up — replace this with real endpoints";
  }
}
