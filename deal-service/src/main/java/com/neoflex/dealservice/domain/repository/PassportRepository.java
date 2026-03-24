package com.neoflex.dealservice.domain.repository;

import com.neoflex.dealservice.domain.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PassportRepository extends JpaRepository<Passport, UUID> {
}