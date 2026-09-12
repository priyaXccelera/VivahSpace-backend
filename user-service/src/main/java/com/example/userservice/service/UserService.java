package com.example.userservice.service;

import com.example.userservice.dto.ChangePasswordRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public User getEntityById(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
  }

  @Transactional(readOnly = true)
  public UserResponse getById(Long id) {
    return UserResponse.from(getEntityById(id));
  }

  @Transactional(readOnly = true)
  public UserResponse getByEmail(String email) {
    User user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email " + email));
    return UserResponse.from(user);
  }

  @Transactional(readOnly = true)
  public boolean existsById(Long id) {
    return userRepository.existsById(id);
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> list(Role role, Pageable pageable) {
    Page<User> page =
        role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
    return page.map(UserResponse::from);
  }

  @Transactional
  public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
    User user = getEntityById(id);
    user.setFullName(request.getFullName().trim());
    user.setPhoneNumber(request.getPhoneNumber());
    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public void changePassword(Long id, ChangePasswordRequest request) {
    User user = getEntityById(id);
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }

  @Transactional
  public void deactivate(Long id) {
    User user = getEntityById(id);
    user.setActive(false);
    userRepository.save(user);
  }

  @Transactional
  public UserResponse reactivate(Long id) {
    User user = getEntityById(id);
    user.setActive(true);
    return UserResponse.from(userRepository.save(user));
  }
}
