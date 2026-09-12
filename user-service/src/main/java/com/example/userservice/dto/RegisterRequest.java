package com.example.userservice.dto;

import com.example.userservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

  @NotBlank(message = "fullName is required")
  @Size(max = 120, message = "fullName must be at most 120 characters")
  private String fullName;

  @NotBlank(message = "email is required")
  @Email(message = "email must be a valid email address")
  @Size(max = 180)
  private String email;

  @NotBlank(message = "password is required")
  @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
  private String password;

  @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "phoneNumber is invalid")
  private String phoneNumber;

  /**
   * Optional. Only CUSTOMER or VENDOR may self-register; ADMIN is rejected. Defaults to CUSTOMER.
   */
  private Role role;

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
