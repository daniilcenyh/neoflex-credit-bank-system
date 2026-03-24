package com.neoflex.dealservice.domain.repository;

import com.neoflex.dealservice.domain.entity.Employment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmploymentRepository extends JpaRepository<Employment, UUID> {
}