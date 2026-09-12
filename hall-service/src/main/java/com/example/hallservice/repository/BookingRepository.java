package com.example.hallservice.repository;

import com.example.hallservice.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  Page<Booking> findByUserId(Long userId, Pageable pageable);

  Page<Booking> findBySlotId(Long slotId, Pageable pageable);
}
