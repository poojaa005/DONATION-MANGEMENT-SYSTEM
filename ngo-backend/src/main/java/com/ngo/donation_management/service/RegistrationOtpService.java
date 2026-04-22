package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.RegistrationOtp;
import com.ngo.donation_management.repository.RegistrationOtpRepository;
import com.ngo.donation_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.security.SecureRandom;

@Service
public class RegistrationOtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    @Autowired
    private RegistrationOtpRepository registrationOtpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void sendRegistrationOtp(String email, String name) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exists");
        }

        String otpCode = String.format("%06d", random.nextInt(1_000_000));
        RegistrationOtp registrationOtp = registrationOtpRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElse(new RegistrationOtp());

        registrationOtp.setEmail(normalizedEmail);
        registrationOtp.setOtpCode(otpCode);
        registrationOtp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        registrationOtpRepository.save(registrationOtp);

        emailService.sendRegistrationOtpEmail(normalizedEmail, name, otpCode, OTP_EXPIRY_MINUTES);
    }

    @Transactional
    public void validateRegistrationOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedOtp = normalizeOtp(otp);

        RegistrationOtp registrationOtp = registrationOtpRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("OTP not requested for this email"));

        if (registrationOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            registrationOtpRepository.delete(registrationOtp);
            throw new RuntimeException("OTP expired. Please request a new OTP");
        }

        if (!Objects.equals(registrationOtp.getOtpCode(), normalizedOtp)) {
            throw new RuntimeException("Invalid OTP");
        }
    }

    @Transactional
    public void clearRegistrationOtp(String email) {
        registrationOtpRepository.deleteByEmailIgnoreCase(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeOtp(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new RuntimeException("OTP is required");
        }
        return otp.trim();
    }
}
