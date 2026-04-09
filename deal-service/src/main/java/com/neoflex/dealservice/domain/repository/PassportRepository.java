package com.neoflex.dealservice.domain.repository;

import com.neoflex.dealservice.domain.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PassportRepository extends JpaRepository<Passport, UUID> {

    Optional<Passport> findBySeriesAndNumber(String series, String number);
    boolean existsBySeriesAndNumber(String series, String number);
}