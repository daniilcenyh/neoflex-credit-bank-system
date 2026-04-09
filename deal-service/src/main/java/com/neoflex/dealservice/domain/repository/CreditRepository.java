package com.neoflex.dealservice.domain.repository;

import com.neoflex.dealservice.domain.entity.Credit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreditRepository extends JpaRepository<Credit, UUID> {
}