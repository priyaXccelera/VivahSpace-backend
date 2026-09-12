package com.example.proposalservice.repository;

import com.example.proposalservice.entity.Proposal;
import com.example.proposalservice.entity.ProposalStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
  Page<Proposal> findByUserId(Long userId, Pageable pageable);

  List<Proposal> findByStatus(ProposalStatus status);
}
