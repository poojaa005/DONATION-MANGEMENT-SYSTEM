package com.ngo.donation_management.entity;

// entity/PickupRequest.java
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "pickup_request")
public class PickupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pickup_id")
    private Integer pickupId;

    @ManyToOne
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @Column(name = "donor_address", nullable = false, columnDefinition = "TEXT")
    private String donorAddress;

    @Column(name = "donor_phone", length = 20)
    private String donorPhone;

    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    @Column(name = "time_slot", length = 50)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_status", nullable = false)
    private PickupStatus pickupStatus;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public enum PickupStatus {
        awaiting_approval, pending, assigned, completed, cancelled
    }

    // Default Constructor
    public PickupRequest() {
    }

    // Parameterized Constructor
    public PickupRequest(Integer pickupId, Donation donation, String donorAddress,
                         String donorPhone,
                         LocalDate pickupDate, String timeSlot,
                         PickupStatus pickupStatus) {
        this.pickupId = pickupId;
        this.donation = donation;
        this.donorAddress = donorAddress;
        this.donorPhone = donorPhone;
        this.pickupDate = pickupDate;
        this.timeSlot = timeSlot;
        this.pickupStatus = pickupStatus;
    }

    // Getters and Setters
    public Integer getPickupId() {
        return pickupId;
    }

    public void setPickupId(Integer pickupId) {
        this.pickupId = pickupId;
    }

    public Donation getDonation() {
        return donation;
    }

    public void setDonation(Donation donation) {
        this.donation = donation;
    }

    public String getDonorAddress() {
        return donorAddress;
    }

    public void setDonorAddress(String donorAddress) {
        this.donorAddress = donorAddress;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public PickupStatus getPickupStatus() {
        return pickupStatus;
    }

    public void setPickupStatus(PickupStatus pickupStatus) {
        this.pickupStatus = pickupStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
