package com.example.userservice.controller;

import com.example.userservice.dto.ChangePasswordRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import com.example.userservice.security.UserPrincipal;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(userService.getById(principal.getId()));
  }

  @PutMapping("/me")
  public ResponseEntity<UserResponse> updateCurrentUser(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
  }

  @PutMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(principal.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/me")
  public ResponseEntity<Void> deactivateCurrentUser(
      @AuthenticationPrincipal UserPrincipal principal) {
    userService.deactivate(principal.getId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getById(id));
  }

  @GetMapping("/{id}/exists")
  public ResponseEntity<Boolean> exists(@PathVariable Long id) {
    return ResponseEntity.ok(userService.existsById(id));
  }

  @GetMapping
  public ResponseEntity<Page<UserResponse>> list(
      @RequestParam(required = false) Role role,
      @org.springframework.data.web.PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(userService.list(role, pageable));
  }

  @PutMapping("/{id}/reactivate")
  public ResponseEntity<UserResponse> reactivate(@PathVariable Long id) {
    return ResponseEntity.ok(userService.reactivate(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    userService.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
