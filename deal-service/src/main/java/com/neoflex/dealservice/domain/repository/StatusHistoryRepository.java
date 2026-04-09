package com.neoflex.dealservice.domain.repository;

import com.neoflex.dealservice.domain.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {
}