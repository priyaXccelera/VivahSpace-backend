package com.example.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.userservice.dto.ChangePasswordRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.GlobalExceptionHandler;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.security.UserPrincipal;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerTest {

  private UserService userService;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private static final Long CURRENT_USER_ID = 1L;

  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);
    UserController controller = new UserController(userService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver(),
                new PageableHandlerMethodArgumentResolver())
            .build();

    User principalUser = new User();
    principalUser.setId(CURRENT_USER_ID);
    principalUser.setFullName("Jane Doe");
    principalUser.setEmail("jane@example.com");
    principalUser.setPasswordHash("hashed");
    principalUser.setRole(Role.CUSTOMER);
    principalUser.setActive(true);
    UserPrincipal principal = new UserPrincipal(principalUser);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private UserResponse buildUserResponse(Long id, String email, boolean active) {
    User user = new User();
    user.setId(id);
    user.setFullName("Jane Doe");
    user.setEmail(email);
    user.setPasswordHash("hashed");
    user.setPhoneNumber("+1 555 0100");
    user.setRole(Role.CUSTOMER);
    user.setActive(active);
    return UserResponse.from(withTimestamps(user));
  }

  private User withTimestamps(User user) {
    try {
      var createdField = User.class.getDeclaredField("createdAt");
      createdField.setAccessible(true);
      createdField.set(user, Instant.parse("2024-01-01T00:00:00Z"));
      var updatedField = User.class.getDeclaredField("updatedAt");
      updatedField.setAccessible(true);
      updatedField.set(user, Instant.parse("2024-01-01T00:00:00Z"));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    return user;
  }

  @Test
  void getCurrentUser_returnsCurrentUserResponse() throws Exception {
    UserResponse response = buildUserResponse(CURRENT_USER_ID, "jane@example.com", true);
    when(userService.getById(CURRENT_USER_ID)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(CURRENT_USER_ID))
        .andExpect(jsonPath("$.email").value("jane@example.com"));
  }

  @Test
  void updateCurrentUser_withValidRequest_returnsUpdatedUser() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("Jane Updated");
    request.setPhoneNumber("+1 555 0199");

    UserResponse response = buildUserResponse(CURRENT_USER_ID, "jane@example.com", true);
    when(userService.updateProfile(eq(CURRENT_USER_ID), any(UpdateProfileRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jane@example.com"));
  }

  @Test
  void updateCurrentUser_withBlankFullName_returnsBadRequest() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("");

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).updateProfile(anyLong(), any(UpdateProfileRequest.class));
  }

  @Test
  void changePassword_withValidRequest_returnsNoContent() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("oldPassword1");
    request.setNewPassword("newPassword1");

    mockMvc
        .perform(
            put("/api/v1/users/me/password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(userService).changePassword(eq(CURRENT_USER_ID), any(ChangePasswordRequest.class));
  }

  @Test
  void changePassword_withWrongCurrentPassword_returnsUnauthorized() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("wrongPassword");
    request.setNewPassword("newPassword1");

    doThrow(new InvalidCredentialsException("Current password is incorrect"))
        .when(userService)
        .changePassword(eq(CURRENT_USER_ID), any(ChangePasswordRequest.class));

    mockMvc
        .perform(
            put("/api/v1/users/me/password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deactivateCurrentUser_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/users/me")).andExpect(status().isNoContent());

    verify(userService).deactivate(CURRENT_USER_ID);
  }

  @Test
  void getById_whenUserExists_returnsUser() throws Exception {
    UserResponse response = buildUserResponse(7L, "bob@example.com", true);
    when(userService.getById(7L)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/{id}", 7L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7L));
  }

  @Test
  void getById_whenUserMissing_returnsNotFound() throws Exception {
    when(userService.getById(404L))
        .thenThrow(new ResourceNotFoundException("User not found with id 404"));

    mockMvc.perform(get("/api/v1/users/{id}", 404L)).andExpect(status().isNotFound());
  }

  @Test
  void exists_returnsBooleanResult() throws Exception {
    when(userService.existsById(3L)).thenReturn(true);

    mockMvc
        .perform(get("/api/v1/users/{id}/exists", 3L))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  void list_returnsPageOfUsers() throws Exception {
    UserResponse response = buildUserResponse(1L, "jane@example.com", true);
    when(userService.list(eq(null), any()))
        .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").value("jane@example.com"));
  }

  @Test
  void reactivate_returnsReactivatedUser() throws Exception {
    UserResponse response = buildUserResponse(5L, "carl@example.com", true);
    when(userService.reactivate(5L)).thenReturn(response);

    mockMvc
        .perform(put("/api/v1/users/{id}/reactivate", 5L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void deactivateById_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/users/{id}", 5L)).andExpect(status().isNoContent());

    verify(userService).deactivate(5L);
  }
}
