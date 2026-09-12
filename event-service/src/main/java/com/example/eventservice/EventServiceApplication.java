package com.example.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class EventServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EventServiceApplication.class, args);
  }
}

// PLACEHOLDER — replace with real entity/DTO/security/controller classes per
// this service's business scope. Keep the /events health-style route
// or replace it once real endpoints exist.
@RestController
class EventServicePlaceholderController {

  @GetMapping("/events")
  public String placeholder() {
    return "event-service is up — replace this with real endpoints";
  }
}
