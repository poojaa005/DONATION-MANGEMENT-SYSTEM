package com.ngo.donation_management.controller;

// controller/DonationReceiptController.java

import com.ngo.donation_management.entity.DonationReceipt;
import com.ngo.donation_management.service.DonationReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
public class DonationReceiptController {

    @Autowired
    private DonationReceiptService donationReceiptService;

    // CREATE
    @PostMapping
    public ResponseEntity<DonationReceipt> createReceipt(
            @RequestBody DonationReceipt receipt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationReceiptService
                        .createReceipt(receipt));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<DonationReceipt>> getAllReceipts() {
        return ResponseEntity.ok(
                donationReceiptService.getAllReceipts());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getReceiptById(
            @PathVariable Integer id) {
        return donationReceiptService.getReceiptById(id)
                .map(r -> ResponseEntity.ok((Object) r))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Receipt not found with id: "
                                        + id)));
    }

    // SEARCH BY DONATION ID
    @GetMapping("/search/donation/{donationId}")
    public ResponseEntity<List<DonationReceipt>> getByDonationId(
            @PathVariable Integer donationId) {
        return ResponseEntity.ok(
                donationReceiptService
                        .getByDonationId(donationId));
    }

    // SEARCH BY RECEIPT NUMBER
    @GetMapping("/search/receipt-number")
    public ResponseEntity<?> getByReceiptNumber(
            @RequestParam String receiptNumber) {
        return donationReceiptService
                .getByReceiptNumber(receiptNumber)
                .map(r -> ResponseEntity.ok((Object) r))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Receipt not found: "
                                        + receiptNumber)));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DonationReceipt> updateReceipt(
            @PathVariable Integer id,
            @RequestBody DonationReceipt receipt) {
        return ResponseEntity.ok(
                donationReceiptService
                        .updateReceipt(id, receipt));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReceipt(
            @PathVariable Integer id) {
        donationReceiptService.deleteReceipt(id);
        return ResponseEntity.ok(Map.of("message",
                "Receipt deleted successfully"));
    }
}