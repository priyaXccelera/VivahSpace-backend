package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class UserServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
}

// PLACEHOLDER — replace with real entity/DTO/security/controller classes per
// this service's business scope. Keep the /users health-style route
// or replace it once real endpoints exist.
@RestController
class UserServicePlaceholderController {

  @GetMapping("/users")
  public String placeholder() {
    return "user-service is up — replace this with real endpoints";
  }
}
