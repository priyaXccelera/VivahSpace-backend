package com.example.eventservice.repository;

import com.example.eventservice.entity.RsvpEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RsvpEntryRepository extends JpaRepository<RsvpEntry, Long> {
  Page<RsvpEntry> findByEventId(Long eventId, Pageable pageable);
}
