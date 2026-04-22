package com.ngo.donation_management.controller;

// controller/DonationController.java
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.service.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    @Autowired
    private DonationService donationService;

    // CREATE (Make Donation)
    @PostMapping
    public ResponseEntity<Donation> createDonation(
            @RequestBody Donation donation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.createDonation(donation));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Donation>> getAllDonations() {
        return ResponseEntity.ok(
                donationService.getAllDonations());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDonationById(
            @PathVariable Integer id) {
        return donationService.getDonationById(id)
                .map(d -> ResponseEntity.ok((Object) d))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Donation not found with id: "
                                        + id)));
    }

    // TRACK DONATION HISTORY (By User ID)
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<List<Donation>> getByUserId(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(
                donationService.getByUserId(userId));
    }

    // SEARCH BY CAMPAIGN ID
    @GetMapping("/search/campaign/{campaignId}")
    public ResponseEntity<List<Donation>> getByCampaignId(
            @PathVariable Integer campaignId) {
        return ResponseEntity.ok(
                donationService.getByCampaignId(campaignId));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<Donation>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                donationService.getByStatus(status));
    }

    // SEARCH BY TYPE
    @GetMapping("/search/type")
    public ResponseEntity<List<Donation>> getByType(
            @RequestParam String type) {
        return ResponseEntity.ok(
                donationService.getByType(type));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Donation> updateDonation(
            @PathVariable Integer id,
            @RequestBody Donation donation) {
        return ResponseEntity.ok(
                donationService.updateDonation(id, donation));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDonation(
            @PathVariable Integer id) {
        donationService.deleteDonation(id);
        return ResponseEntity.ok(Map.of("message",
                "Donation deleted successfully"));
    }
}