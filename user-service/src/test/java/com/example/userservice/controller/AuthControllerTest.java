package com.example.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.EmailAlreadyExistsException;
import com.example.userservice.exception.GlobalExceptionHandler;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

  private AuthService authService;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    authService = mock(AuthService.class);
    AuthController controller = new AuthController(authService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private UserResponse buildUserResponse(Long id, String email) {
    User user = new User();
    user.setId(id);
    user.setFullName("Jane Doe");
    user.setEmail(email);
    user.setPasswordHash("hashed");
    user.setRole(Role.CUSTOMER);
    user.setActive(true);
    return UserResponse.from(user);
  }

  @Test
  void register_withValidRequest_returnsCreatedWithToken() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("Jane Doe");
    request.setEmail("jane@example.com");
    request.setPassword("secretPass1");

    AuthResponse response =
        new AuthResponse("jwt-token", 1_800_000L, buildUserResponse(1L, "jane@example.com"));
    when(authService.register(any(RegisterRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("jwt-token"))
        .andExpect(jsonPath("$.user.email").value("jane@example.com"));
  }

  @Test
  void register_withMissingEmail_returnsBadRequest() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("Jane Doe");
    request.setPassword("secretPass1");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(authService, never()).register(any(RegisterRequest.class));
  }

  @Test
  void register_withDuplicateEmail_returnsConflict() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("Jane Doe");
    request.setEmail("jane@example.com");
    request.setPassword("secretPass1");

    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(
            new EmailAlreadyExistsException(
                "An account with email jane@example.com already exists"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void login_withValidCredentials_returnsOkWithToken() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("jane@example.com");
    request.setPassword("secretPass1");

    AuthResponse response =
        new AuthResponse("jwt-token", 1_800_000L, buildUserResponse(1L, "jane@example.com"));
    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("jwt-token"));
  }

  @Test
  void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("jane@example.com");
    request.setPassword("wrongPassword");

    when(authService.login(any(LoginRequest.class)))
        .thenThrow(new InvalidCredentialsException("Invalid email or password"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_withMissingPassword_returnsBadRequest() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("jane@example.com");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(authService, never()).login(any(LoginRequest.class));
  }
}
