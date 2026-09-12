package com.example.eventservice.repository;

import com.example.eventservice.entity.ItineraryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryEntryRepository extends JpaRepository<ItineraryEntry, Long> {
  Page<ItineraryEntry> findByEventId(Long eventId, Pageable pageable);
}
