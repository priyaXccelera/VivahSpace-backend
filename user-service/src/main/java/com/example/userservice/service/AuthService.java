package com.example.userservice.service;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.exception.EmailAlreadyExistsException;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String normalizedEmail = request.getEmail().trim().toLowerCase();

    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new EmailAlreadyExistsException(
          "An account with email " + normalizedEmail + " already exists");
    }

    Role requestedRole = request.getRole() == null ? Role.CUSTOMER : request.getRole();

    User user = new User();
    user.setFullName(request.getFullName().trim());
    user.setEmail(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPhoneNumber(request.getPhoneNumber());
    user.setRole(requestedRole);
    user.setActive(true);

    User saved = userRepository.save(user);
    String token = jwtService.generateToken(saved);
    return new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.from(saved));
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailIgnoreCase(request.getEmail().trim())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    if (!user.isActive()) {
      throw new InvalidCredentialsException("This account has been deactivated");
    }

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    String token = jwtService.generateToken(user);
    return new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.from(user));
  }
}
