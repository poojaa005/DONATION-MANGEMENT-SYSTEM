package com.ngo.donation_management.controller;

// controller/AuthController.java

import com.ngo.donation_management.dto.AuthRequest;
import com.ngo.donation_management.dto.AuthResponse;
import com.ngo.donation_management.dto.RegisterRequest;
import com.ngo.donation_management.dto.RegistrationOtpRequest;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.service.RegistrationOtpService;
import com.ngo.donation_management.service.NotificationService;
import com.ngo.donation_management.service.UserService;
import com.ngo.donation_management.service.VolunteerService;
import com.ngo.donation_management.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private RegistrationOtpService registrationOtpService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/register/request-otp")
    public ResponseEntity<?> requestRegistrationOtp(
            @RequestBody RegistrationOtpRequest request) {
        try {
            registrationOtpService.sendRegistrationOtp(
                    request.getEmail(), request.getName());
            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to your email",
                    "email", request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {
        try {
            User.Role requestedRole = User.Role.valueOf(
                    request.getRole().toLowerCase());

            if (requestedRole != User.Role.donor
                    && requestedRole != User.Role.volunteer) {
                return ResponseEntity.badRequest().body(
                        Map.of("error",
                                "Only donor and volunteer registration is allowed here"));
            }

            if (userService.emailExists(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Email already exists"));
            }

            registrationOtpService.validateRegistrationOtp(
                    request.getEmail(), request.getOtp());

            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setPassword(request.getPassword());
            user.setAddress(request.getAddress());
            user.setCity(request.getCity());
            user.setRole(requestedRole);

            User savedUser = userService.createUser(user);
            registrationOtpService.clearRegistrationOtp(
                    savedUser.getEmail());

            if (savedUser.getRole() == User.Role.volunteer) {
                volunteerService.ensureVolunteerProfile(savedUser);
                notificationService.notifyVolunteerRegistrationSuccess(savedUser);
            } else {
                notificationService.notifyDonorRegistrationSuccess(savedUser);
            }

            String token = jwtUtil.generateToken(
                    savedUser.getEmail(),
                    savedUser.getRole().name());

            AuthResponse response = buildAuthResponse(savedUser, token);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody AuthRequest request) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail(),
                                    request.getPassword()
                            )
                    );

            User user = userService
                    .getUserByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException(
                            "User not found"));

            if (user.getRole() == User.Role.ngo_admin
                    && (user.getNgo() == null || user.getNgo().getNgoId() == null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error",
                                "NGO admin login is allowed only after assignment to an NGO"));
            }

            if (user.getRole() == User.Role.volunteer) {
                volunteerService.ensureVolunteerProfile(user);
            }

            String token = jwtUtil.generateToken(
                    user.getEmail(), user.getRole().name());

            AuthResponse response = buildAuthResponse(user, token);

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error",
                            "Invalid email or password"));
        }
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getName(),
                user.getNgo() != null ? user.getNgo().getNgoId() : null,
                user.getNgo() != null ? user.getNgo().getNgoName() : null
        );
    }
}
