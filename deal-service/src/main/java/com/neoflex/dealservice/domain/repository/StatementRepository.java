package com.neoflex.dealservice.domain.repository;

import com.neoflex.dealservice.domain.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {
}