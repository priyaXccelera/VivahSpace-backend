package com.example.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.userservice.dto.ChangePasswordRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder);
  }

  private User buildUser(Long id, String email, boolean active) {
    User user = new User();
    user.setId(id);
    user.setFullName("Jane Doe");
    user.setEmail(email);
    user.setPasswordHash("hashed-password");
    user.setPhoneNumber("+1 555 0100");
    user.setRole(Role.CUSTOMER);
    user.setActive(active);
    return user;
  }

  @Test
  void getEntityById_whenUserExists_returnsUser() {
    User user = buildUser(1L, "jane@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    User result = userService.getEntityById(1L);

    assertThat(result).isSameAs(user);
  }

  @Test
  void getEntityById_whenUserMissing_throwsResourceNotFoundException() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getEntityById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void getById_returnsMappedUserResponse() {
    User user = buildUser(2L, "bob@example.com", true);
    when(userRepository.findById(2L)).thenReturn(Optional.of(user));

    UserResponse response = userService.getById(2L);

    assertThat(response.getId()).isEqualTo(2L);
    assertThat(response.getEmail()).isEqualTo("bob@example.com");
    assertThat(response.getFullName()).isEqualTo("Jane Doe");
  }

  @Test
  void getByEmail_whenFound_returnsMappedUserResponse() {
    User user = buildUser(3L, "alice@example.com", true);
    when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

    UserResponse response = userService.getByEmail("alice@example.com");

    assertThat(response.getEmail()).isEqualTo("alice@example.com");
  }

  @Test
  void getByEmail_whenMissing_throwsResourceNotFoundException() {
    when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getByEmail("missing@example.com"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void existsById_delegatesToRepository() {
    when(userRepository.existsById(5L)).thenReturn(true);

    assertThat(userService.existsById(5L)).isTrue();
    verify(userRepository).existsById(5L);
  }

  @Test
  void list_withNullRole_callsFindAll() {
    Pageable pageable = PageRequest.of(0, 20);
    User user = buildUser(1L, "jane@example.com", true);
    Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
    when(userRepository.findAll(pageable)).thenReturn(page);

    Page<UserResponse> result = userService.list(null, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(userRepository).findAll(pageable);
    verify(userRepository, never()).findByRole(any(), any());
  }

  @Test
  void list_withRole_callsFindByRole() {
    Pageable pageable = PageRequest.of(0, 20);
    User user = buildUser(1L, "admin@example.com", true);
    user.setRole(Role.ADMIN);
    Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
    when(userRepository.findByRole(Role.ADMIN, pageable)).thenReturn(page);

    Page<UserResponse> result = userService.list(Role.ADMIN, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getRole()).isEqualTo(Role.ADMIN);
    verify(userRepository).findByRole(Role.ADMIN, pageable);
    verify(userRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void updateProfile_updatesFullNameAndPhoneNumber() {
    User user = buildUser(1L, "jane@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("  Jane Updated  ");
    request.setPhoneNumber("+1 555 0199");

    UserResponse response = userService.updateProfile(1L, request);

    assertThat(response.getFullName()).isEqualTo("Jane Updated");
    assertThat(response.getPhoneNumber()).isEqualTo("+1 555 0199");
    verify(userRepository).save(user);
  }

  @Test
  void changePassword_whenCurrentPasswordMatches_updatesHash() {
    User user = buildUser(1L, "jane@example.com", true);
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("oldPass123");
    request.setNewPassword("newPass456");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("oldPass123", "hashed-password")).thenReturn(true);
    when(passwordEncoder.encode("newPass456")).thenReturn("new-hashed-password");

    userService.changePassword(1L, request);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hashed-password");
  }

  @Test
  void changePassword_whenCurrentPasswordWrong_throwsInvalidCredentialsException() {
    User user = buildUser(1L, "jane@example.com", true);
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("wrongPass");
    request.setNewPassword("newPass456");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPass", "hashed-password")).thenReturn(false);

    assertThatThrownBy(() -> userService.changePassword(1L, request))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void deactivate_setsActiveFalseAndSaves() {
    User user = buildUser(1L, "jane@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    userService.deactivate(1L);

    assertThat(user.isActive()).isFalse();
    verify(userRepository).save(user);
  }

  @Test
  void reactivate_setsActiveTrueAndReturnsResponse() {
    User user = buildUser(1L, "jane@example.com", false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.reactivate(1L);

    assertThat(user.isActive()).isTrue();
    assertThat(response.isActive()).isTrue();
  }
}
