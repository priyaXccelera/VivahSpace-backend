package com.example.hallservice.repository;

import com.example.hallservice.entity.HallAvailabilitySlot;
import com.example.hallservice.entity.SlotStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallAvailabilitySlotRepository extends JpaRepository<HallAvailabilitySlot, Long> {

  Page<HallAvailabilitySlot> findByHallId(Long hallId, Pageable pageable);

  List<HallAvailabilitySlot> findByHallIdAndSlotDateAndStatus(
      Long hallId, LocalDate slotDate, SlotStatus status);

  List<HallAvailabilitySlot> findBySlotDateAndStatus(LocalDate slotDate, SlotStatus status);

  boolean existsByHallIdAndSlotDateAndSlotType(
      Long hallId, LocalDate slotDate, com.example.hallservice.entity.SlotType slotType);
}
