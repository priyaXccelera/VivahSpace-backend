package com.example.userservice.repository;

import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByRole(Role role);

  Page<User> findByRole(Role role, Pageable pageable);
}
