package com.example.eventservice.repository;

import com.example.eventservice.entity.ChecklistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
  Page<ChecklistItem> findByEventId(Long eventId, Pageable pageable);
}
