package com.ngo.donation_management.controller;

// controller/PickupRequestController.java
import com.ngo.donation_management.dto.RejectionRequest;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.service.PickupRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pickups")
public class PickupRequestController {

    @Autowired
    private PickupRequestService pickupRequestService;

    // CREATE (Schedule Pickup)
    @PostMapping
    public ResponseEntity<PickupRequest> createPickupRequest(
            @RequestBody PickupRequest pickupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pickupRequestService
                        .createPickupRequest(pickupRequest));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<PickupRequest>>
    getAllPickupRequests() {
        return ResponseEntity.ok(
                pickupRequestService.getAllPickupRequests());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPickupRequestById(
            @PathVariable Integer id) {
        return pickupRequestService.getPickupRequestById(id)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Pickup not found with id: "
                                        + id)));
    }

    // SEARCH BY DONATION ID
    @GetMapping("/search/donation/{donationId}")
    public ResponseEntity<List<PickupRequest>> getByDonationId(
            @PathVariable Integer donationId) {
        return ResponseEntity.ok(
                pickupRequestService
                        .getByDonationId(donationId));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<PickupRequest>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                pickupRequestService.getByStatus(status));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<PickupRequest> updatePickupRequest(
            @PathVariable Integer id,
            @RequestBody PickupRequest pickupRequest) {
        return ResponseEntity.ok(
                pickupRequestService
                        .updatePickupRequest(id, pickupRequest));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PickupRequest> approvePickupRequest(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                pickupRequestService.approvePickup(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<PickupRequest> rejectPickupRequest(
            @PathVariable Integer id,
            @RequestBody RejectionRequest rejectionRequest) {
        return ResponseEntity.ok(
                pickupRequestService.rejectPickup(
                        id,
                        rejectionRequest != null ? rejectionRequest.getReason() : null));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePickupRequest(
            @PathVariable Integer id) {
        pickupRequestService.deletePickupRequest(id);
        return ResponseEntity.ok(Map.of("message",
                "Pickup request deleted successfully"));
    }
}
