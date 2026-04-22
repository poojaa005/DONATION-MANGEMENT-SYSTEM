package com.ngo.donation_management.controller;

// controller/VolunteerController.java

import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {

    @Autowired
    private VolunteerService volunteerService;

    // CREATE
    @PostMapping
    public ResponseEntity<Volunteer> createVolunteer(
            @RequestBody Volunteer volunteer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(volunteerService.createVolunteer(volunteer));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Volunteer>> getAllVolunteers() {
        return ResponseEntity.ok(
                volunteerService.getAllVolunteers());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getVolunteerById(
            @PathVariable Integer id) {
        return volunteerService.getVolunteerById(id)
                .map(vol -> ResponseEntity.ok((Object) vol))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Volunteer not found with id: "
                                        + id)));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<Volunteer>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                volunteerService.getByStatus(status));
    }

    // SEARCH BY NGO ID
    @GetMapping("/search/ngo/{ngoId}")
    public ResponseEntity<List<Volunteer>> getByNgoId(
            @PathVariable Integer ngoId) {
        return ResponseEntity.ok(
                volunteerService.getByNgoId(ngoId));
    }

    // SEARCH BY USER ID
    @GetMapping("/search/user/{userId}")
    public ResponseEntity<?> getByUserId(
            @PathVariable Integer userId) {
        return volunteerService.getByUserId(userId)
                .map(vol -> ResponseEntity.ok((Object) vol))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Volunteer not found for user: "
                                        + userId)));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Volunteer> updateVolunteer(
            @PathVariable Integer id,
            @RequestBody Volunteer volunteer) {
        return ResponseEntity.ok(
                volunteerService.updateVolunteer(id, volunteer));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVolunteer(
            @PathVariable Integer id) {
        volunteerService.deleteVolunteer(id);
        return ResponseEntity.ok(Map.of("message",
                "Volunteer deleted successfully"));
    }
}