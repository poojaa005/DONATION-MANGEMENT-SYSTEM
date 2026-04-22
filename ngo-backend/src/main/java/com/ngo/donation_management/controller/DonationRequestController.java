package com.ngo.donation_management.controller;

// controller/DonationRequestController.java
import com.ngo.donation_management.dto.RejectionRequest;
import com.ngo.donation_management.entity.DonationRequest;
import com.ngo.donation_management.service.DonationRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donation-requests")
public class DonationRequestController {

    @Autowired
    private DonationRequestService donationRequestService;

    // CREATE
    @PostMapping
    public ResponseEntity<DonationRequest> createDonationRequest(
            @RequestBody DonationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationRequestService
                        .createDonationRequest(request));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<DonationRequest>>
    getAllDonationRequests() {
        return ResponseEntity.ok(
                donationRequestService
                        .getAllDonationRequests());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDonationRequestById(
            @PathVariable Integer id) {
        return donationRequestService
                .getDonationRequestById(id)
                .map(req -> ResponseEntity.ok((Object) req))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Donation request not found: "
                                        + id)));
    }

    // SEARCH BY USER ID
    @GetMapping("/search/user/{userId}")
    public ResponseEntity<List<DonationRequest>> getByUserId(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(
                donationRequestService.getByUserId(userId));
    }

    // SEARCH BY CAMPAIGN ID
    @GetMapping("/search/campaign/{campaignId}")
    public ResponseEntity<List<DonationRequest>> getByCampaignId(
            @PathVariable Integer campaignId) {
        return ResponseEntity.ok(
                donationRequestService
                        .getByCampaignId(campaignId));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<DonationRequest>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                donationRequestService.getByStatus(status));
    }

    // SEARCH BY DONATION TYPE
    @GetMapping("/search/type")
    public ResponseEntity<List<DonationRequest>> getByDonationType(
            @RequestParam String type) {
        return ResponseEntity.ok(
                donationRequestService.getByDonationType(type));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DonationRequest> updateDonationRequest(
            @PathVariable Integer id,
            @RequestBody DonationRequest request) {
        return ResponseEntity.ok(
                donationRequestService
                        .updateDonationRequest(id, request));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGO_ADMIN')")
    public ResponseEntity<?> approveDonationRequest(
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(
                    donationRequestService.approveRequest(id));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", exception.getMessage()
            ));
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGO_ADMIN')")
    public ResponseEntity<?> rejectDonationRequest(
            @PathVariable Integer id,
            @RequestBody RejectionRequest rejectionRequest) {
        try {
            return ResponseEntity.ok(
                    donationRequestService.rejectRequest(
                            id,
                            rejectionRequest != null ? rejectionRequest.getReason() : null));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", exception.getMessage()
            ));
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDonationRequest(
            @PathVariable Integer id) {
        donationRequestService.deleteDonationRequest(id);
        return ResponseEntity.ok(Map.of("message",
                "Donation request deleted successfully"));
    }
}
