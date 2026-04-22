package com.ngo.donation_management.controller;

// controller/UrgentNeedsController.java
import com.ngo.donation_management.entity.UrgentNeeds;
import com.ngo.donation_management.service.UrgentNeedsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urgent-needs")
public class UrgentNeedsController {

    @Autowired
    private UrgentNeedsService urgentNeedsService;

    // CREATE
    @PostMapping
    public ResponseEntity<UrgentNeeds> createUrgentNeed(
            @RequestBody UrgentNeeds urgentNeed) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(urgentNeedsService
                        .createUrgentNeed(urgentNeed));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<UrgentNeeds>> getAllUrgentNeeds() {
        return ResponseEntity.ok(
                urgentNeedsService.getAllUrgentNeeds());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUrgentNeedById(
            @PathVariable Integer id) {
        return urgentNeedsService.getUrgentNeedById(id)
                .map(need -> ResponseEntity.ok((Object) need))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Urgent need not found with id: "
                                        + id)));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<UrgentNeeds>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                urgentNeedsService.getByStatus(status));
    }

    // SEARCH BY TITLE
    @GetMapping("/search/title")
    public ResponseEntity<List<UrgentNeeds>> searchByTitle(
            @RequestParam String title) {
        return ResponseEntity.ok(
                urgentNeedsService.searchByTitle(title));
    }

    // SEARCH BY ADMIN ID
    @GetMapping("/search/admin/{adminId}")
    public ResponseEntity<List<UrgentNeeds>> getByAdminId(
            @PathVariable Integer adminId) {
        return ResponseEntity.ok(
                urgentNeedsService.getByAdminId(adminId));
    }

    // GET OPEN URGENT NEEDS (Guest feature)
    @GetMapping("/open")
    public ResponseEntity<List<UrgentNeeds>> getOpenNeeds() {
        return ResponseEntity.ok(
                urgentNeedsService.getByStatus("open"));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<UrgentNeeds> updateUrgentNeed(
            @PathVariable Integer id,
            @RequestBody UrgentNeeds urgentNeed) {
        return ResponseEntity.ok(
                urgentNeedsService
                        .updateUrgentNeed(id, urgentNeed));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUrgentNeed(
            @PathVariable Integer id) {
        urgentNeedsService.deleteUrgentNeed(id);
        return ResponseEntity.ok(Map.of("message",
                "Urgent need deleted successfully"));
    }
}