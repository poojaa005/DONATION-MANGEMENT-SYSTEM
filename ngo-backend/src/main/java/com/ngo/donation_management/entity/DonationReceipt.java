package com.ngo.donation_management.entity;

// entity/DonationReceipt.java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation_receipt")
public class DonationReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Integer receiptId;

    @ManyToOne
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 100)
    private String receiptNumber;

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "certificate_url", length = 255)
    private String certificateUrl;

    // Default Constructor
    public DonationReceipt() {
    }

    // Parameterized Constructor
    public DonationReceipt(Integer receiptId, Donation donation,
                           String receiptNumber, LocalDateTime issuedDate,
                           String certificateUrl) {
        this.receiptId = receiptId;
        this.donation = donation;
        this.receiptNumber = receiptNumber;
        this.issuedDate = issuedDate;
        this.certificateUrl = certificateUrl;
    }

    // Getters and Setters
    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
    }

    public Donation getDonation() {
        return donation;
    }

    public void setDonation(Donation donation) {
        this.donation = donation;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDateTime issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.issuedDate = LocalDateTime.now();
    }
}