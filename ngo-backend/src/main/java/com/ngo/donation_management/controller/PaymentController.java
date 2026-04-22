package com.ngo.donation_management.controller;

// controller/PaymentController.java
import com.ngo.donation_management.entity.Payment;
import com.ngo.donation_management.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // CREATE
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestBody Payment payment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(payment));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(
                paymentService.getAllPayments());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(
            @PathVariable Integer id) {
        return paymentService.getPaymentById(id)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Payment not found with id: "
                                        + id)));
    }

    // SEARCH BY DONATION ID
    @GetMapping("/search/donation/{donationId}")
    public ResponseEntity<List<Payment>> getByDonationId(
            @PathVariable Integer donationId) {
        return ResponseEntity.ok(
                paymentService.getByDonationId(donationId));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<Payment>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                paymentService.getByStatus(status));
    }

    // SEARCH BY METHOD
    @GetMapping("/search/method")
    public ResponseEntity<List<Payment>> getByMethod(
            @RequestParam String method) {
        return ResponseEntity.ok(
                paymentService.getByMethod(method));
    }

    // SEARCH BY TRANSACTION ID
    @GetMapping("/search/transaction")
    public ResponseEntity<?> getByTransactionId(
            @RequestParam String transactionId) {
        return paymentService.getByTransactionId(transactionId)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Payment not found with txn: "
                                        + transactionId)));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(
            @PathVariable Integer id,
            @RequestBody Payment payment) {
        return ResponseEntity.ok(
                paymentService.updatePayment(id, payment));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(
            @PathVariable Integer id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok(Map.of("message",
                "Payment deleted successfully"));
    }
}