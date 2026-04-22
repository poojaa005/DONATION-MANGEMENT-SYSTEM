package com.ngo.donation_management.entity;

// entity/Donation.java
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_id")
    private Integer donationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "ngo_id")
    private Ngo ngo;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_type", nullable = false)
    private DonationType donationType;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_status", nullable = false)
    private DonationStatus donationStatus;

    @Column(name = "donation_date")
    private LocalDateTime donationDate;

    public enum DonationType {
        monetary, goods
    }

    public enum DonationStatus {
        pending, approved, completed, cancelled
    }

    // Default Constructor
    public Donation() {
    }

    // Parameterized Constructor
    public Donation(Integer donationId, User user, Campaign campaign,
                    DonationType donationType, BigDecimal amount,
                    DonationStatus donationStatus, LocalDateTime donationDate) {
        this.donationId = donationId;
        this.user = user;
        this.campaign = campaign;
        this.donationType = donationType;
        this.amount = amount;
        this.donationStatus = donationStatus;
        this.donationDate = donationDate;
    }

    // Getters and Setters
    public Integer getDonationId() {
        return donationId;
    }

    public void setDonationId(Integer donationId) {
        this.donationId = donationId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Ngo getNgo() {
        return ngo;
    }

    public void setNgo(Ngo ngo) {
        this.ngo = ngo;
    }

    public DonationType getDonationType() {
        return donationType;
    }

    public void setDonationType(DonationType donationType) {
        this.donationType = donationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public DonationStatus getDonationStatus() {
        return donationStatus;
    }

    public void setDonationStatus(DonationStatus donationStatus) {
        this.donationStatus = donationStatus;
    }

    public LocalDateTime getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDateTime donationDate) {
        this.donationDate = donationDate;
    }

    @PrePersist
    protected void onCreate() {
        this.donationDate = LocalDateTime.now();
    }
}
