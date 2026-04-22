package com.ngo.donation_management.controller;

// controller/UserController.java

import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedUser);
    }

    // READ ALL
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // READ BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok((Object) user))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "User not found with id: " + id)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('DONOR', 'VOLUNTEER', 'ADMIN', 'NGO_ADMIN')")
    public ResponseEntity<User> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    // SEARCH BY NAME
    @GetMapping("/search/name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchByName(
            @RequestParam String name) {
        return ResponseEntity.ok(userService.searchByName(name));
    }

    // SEARCH BY EMAIL
    @GetMapping("/search/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchByEmail(
            @RequestParam String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok((Object) user))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "User not found with email: "
                                        + email)));
    }

    // SEARCH BY CITY
    @GetMapping("/search/city")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchByCity(
            @RequestParam String city) {
        return ResponseEntity.ok(userService.searchByCity(city));
    }

    // SEARCH BY ROLE
    @GetMapping("/search/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchByRole(
            @RequestParam String role) {
        return ResponseEntity.ok(userService.searchByRole(role));
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(
            @PathVariable Integer id, @RequestBody User user) {
        return ResponseEntity.ok(
                userService.updateUser(id, user));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('DONOR', 'VOLUNTEER', 'ADMIN', 'NGO_ADMIN')")
    public ResponseEntity<User> updateCurrentUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.updateCurrentUser(user));
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message",
                    "User deleted successfully"));
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", exception.getMessage()));
        } catch (RuntimeException exception) {
            HttpStatus status = exception.getMessage() != null
                    && exception.getMessage().contains("User not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
