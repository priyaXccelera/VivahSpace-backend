package com.example.proposalservice.repository;

import com.example.proposalservice.entity.ProposalItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalItemRepository extends JpaRepository<ProposalItem, Long> {}
