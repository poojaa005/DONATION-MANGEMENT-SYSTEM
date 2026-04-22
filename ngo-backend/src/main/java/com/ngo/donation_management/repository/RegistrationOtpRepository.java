package com.ngo.donation_management.repository;

import com.ngo.donation_management.entity.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, Integer> {
    Optional<RegistrationOtp> findByEmailIgnoreCase(String email);

    void deleteByEmailIgnoreCase(String email);
}
