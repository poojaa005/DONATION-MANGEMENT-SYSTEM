package com.ngo.donation_management.controller;

// controller/DonationItemController.java
import com.ngo.donation_management.entity.DonationItem;
import com.ngo.donation_management.service.DonationItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donation-items")
public class DonationItemController {

    @Autowired
    private DonationItemService donationItemService;

    // CREATE
    @PostMapping
    public ResponseEntity<DonationItem> createDonationItem(
            @RequestBody DonationItem item) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationItemService.createDonationItem(item));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<DonationItem>>
    getAllDonationItems() {
        return ResponseEntity.ok(
                donationItemService.getAllDonationItems());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDonationItemById(
            @PathVariable Integer id) {
        return donationItemService.getDonationItemById(id)
                .map(item -> ResponseEntity.ok((Object) item))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Item not found with id: " + id)));
    }

    // SEARCH BY DONATION ID
    @GetMapping("/search/donation/{donationId}")
    public ResponseEntity<List<DonationItem>> getByDonationId(
            @PathVariable Integer donationId) {
        return ResponseEntity.ok(
                donationItemService.getByDonationId(donationId));
    }

    // SEARCH BY CATEGORY
    @GetMapping("/search/category")
    public ResponseEntity<List<DonationItem>> getByCategory(
            @RequestParam String category) {
        return ResponseEntity.ok(
                donationItemService.getByCategory(category));
    }

    // SEARCH BY ITEM NAME
    @GetMapping("/search/name")
    public ResponseEntity<List<DonationItem>> searchByItemName(
            @RequestParam String name) {
        return ResponseEntity.ok(
                donationItemService.searchByItemName(name));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DonationItem> updateDonationItem(
            @PathVariable Integer id,
            @RequestBody DonationItem item) {
        return ResponseEntity.ok(
                donationItemService
                        .updateDonationItem(id, item));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDonationItem(
            @PathVariable Integer id) {
        donationItemService.deleteDonationItem(id);
        return ResponseEntity.ok(Map.of("message",
                "Donation item deleted successfully"));
    }
}