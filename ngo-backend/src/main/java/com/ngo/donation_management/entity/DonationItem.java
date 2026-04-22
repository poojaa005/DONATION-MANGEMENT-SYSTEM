package com.ngo.donation_management.entity;

// entity/DonationItem.java

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "donation_item")
public class DonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    // Default Constructor
    public DonationItem() {
    }

    // Parameterized Constructor
    public DonationItem(Integer itemId, Donation donation, String itemName,
                        String category, Integer quantity, String description,
                        BigDecimal estimatedValue) {
        this.itemId = itemId;
        this.donation = donation;
        this.itemName = itemName;
        this.category = category;
        this.quantity = quantity;
        this.description = description;
        this.estimatedValue = estimatedValue;
    }

    // Getters and Setters
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Donation getDonation() {
        return donation;
    }

    public void setDonation(Donation donation) {
        this.donation = donation;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
}