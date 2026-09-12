package com.example.hallservice.repository;

import com.example.hallservice.entity.Waitlist;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
  Page<Waitlist> findByHallId(Long hallId, Pageable pageable);

  Page<Waitlist> findBySlotId(Long slotId, Pageable pageable);

  List<Waitlist> findBySlotIdAndNotifiedFalseOrderByJoinedAtAsc(Long slotId);
}
