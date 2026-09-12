package com.example.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.EmailAlreadyExistsException;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.exception.InvalidRoleException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userRepository, passwordEncoder, jwtService);
  }

  private User buildUser(Long id, String email, String passwordHash, Role role, boolean active) {
    User user = new User();
    user.setId(id);
    user.setFullName("Jane Doe");
    user.setEmail(email);
    user.setPasswordHash(passwordHash);
    user.setRole(role);
    user.setActive(active);
    return user;
  }

  @Test
  void register_whenEmailNotTaken_createsUserWithDefaultRoleAndReturnsToken() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("  Jane Doe  ");
    request.setEmail("  Jane@Example.com  ");
    request.setPassword("secretPass1");

    when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
    when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);
    when(passwordEncoder.encode("secretPass1")).thenReturn("hashed");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User u = invocation.getArgument(0);
              u.setId(10L);
              return u;
            });
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    when(jwtService.getExpirationMs()).thenReturn(1_800_000L);

    AuthResponse response = authService.register(request);

    assertThat(response.getAccessToken()).isEqualTo("jwt-token");
    assertThat(response.getExpiresInMs()).isEqualTo(1_800_000L);
    assertThat(response.getUser().getEmail()).isEqualTo("jane@example.com");
    assertThat(response.getUser().getFullName()).isEqualTo("Jane Doe");
    assertThat(response.getUser().getRole()).isEqualTo(Role.CUSTOMER);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
    assertThat(captor.getValue().isActive()).isTrue();
  }

  @Test
  void register_whenNoAdminExists_promotesFirstUserToAdmin() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("First User");
    request.setEmail("first@example.com");
    request.setPassword("secretPass1");

    when(userRepository.existsByEmailIgnoreCase("first@example.com")).thenReturn(false);
    when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
    when(passwordEncoder.encode("secretPass1")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    when(jwtService.getExpirationMs()).thenReturn(1_800_000L);

    AuthResponse response = authService.register(request);

    assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  void register_whenNoAdminExists_promotesFirstUserEvenIfCustomerRequested() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("First User");
    request.setEmail("first@example.com");
    request.setPassword("secretPass1");
    request.setRole(Role.CUSTOMER);

    when(userRepository.existsByEmailIgnoreCase("first@example.com")).thenReturn(false);
    when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
    when(passwordEncoder.encode("secretPass1")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    when(jwtService.getExpirationMs()).thenReturn(1_800_000L);

    AuthResponse response = authService.register(request);

    assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  void register_whenAdminAlreadyExistsAndAdminRequested_throwsInvalidRoleException() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("Impostor");
    request.setEmail("impostor@example.com");
    request.setPassword("secretPass1");
    request.setRole(Role.ADMIN);

    when(userRepository.existsByEmailIgnoreCase("impostor@example.com")).thenReturn(false);
    when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(InvalidRoleException.class)
        .hasMessageContaining("self-registration");

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void register_whenEmailAlreadyExists_throwsEmailAlreadyExistsException() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName("Jane Doe");
    request.setEmail("jane@example.com");
    request.setPassword("secretPass1");

    when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining("jane@example.com");

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void login_withValidCredentials_returnsAuthResponse() {
    User user = buildUser(1L, "jane@example.com", "hashed-password", Role.CUSTOMER, true);
    LoginRequest request = new LoginRequest();
    request.setEmail("jane@example.com");
    request.setPassword("plainPassword");

    when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("plainPassword", "hashed-password")).thenReturn(true);
    when(jwtService.generateToken(user)).thenReturn("jwt-token");
    when(jwtService.getExpirationMs()).thenReturn(1_800_000L);

    AuthResponse response = authService.login(request);

    assertThat(response.getAccessToken()).isEqualTo("jwt-token");
    assertThat(response.getUser().getEmail()).isEqualTo("jane@example.com");
  }

  @Test
  void login_whenUserNotFound_throwsInvalidCredentialsException() {
    LoginRequest request = new LoginRequest();
    request.setEmail("missing@example.com");
    request.setPassword("plainPassword");

    when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void login_whenUserInactive_throwsInvalidCredentialsException() {
    User user = buildUser(1L, "jane@example.com", "hashed-password", Role.CUSTOMER, false);
    LoginRequest request = new LoginRequest();
    request.setEmail("jane@example.com");
    request.setPassword("plainPassword");

    when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessageContaining("deactivated");

    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  void login_whenPasswordInvalid_throwsInvalidCredentialsException() {
    User user = buildUser(1L, "jane@example.com", "hashed-password", Role.CUSTOMER, true);
    LoginRequest request = new LoginRequest();
    request.setEmail("jane@example.com");
    request.setPassword("wrongPassword");

    when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPassword", "hashed-password")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(jwtService, never()).generateToken(any(User.class));
  }
}
