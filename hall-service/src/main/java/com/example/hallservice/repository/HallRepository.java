package com.example.hallservice.repository;

import com.example.hallservice.entity.Hall;
import com.example.hallservice.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallRepository extends JpaRepository<Hall, Long> {
  Page<Hall> findByVerificationStatus(VerificationStatus status, Pageable pageable);
}
