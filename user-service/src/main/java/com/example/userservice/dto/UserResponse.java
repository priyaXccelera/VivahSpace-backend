package com.example.userservice.dto;

import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import java.time.Instant;

public class UserResponse {

  private Long id;
  private String fullName;
  private String email;
  private String phoneNumber;
  private Role role;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  public static UserResponse from(User user) {
    UserResponse dto = new UserResponse();
    dto.id = user.getId();
    dto.fullName = user.getFullName();
    dto.email = user.getEmail();
    dto.phoneNumber = user.getPhoneNumber();
    dto.role = user.getRole();
    dto.active = user.isActive();
    dto.createdAt = user.getCreatedAt();
    dto.updatedAt = user.getUpdatedAt();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public Role getRole() {
    return role;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
