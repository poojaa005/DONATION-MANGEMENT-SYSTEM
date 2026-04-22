package com.ngo.donation_management.dto;

// dto/ReportDTO.java
import java.math.BigDecimal;

public class ReportDTO {
    private long totalDonations;
    private BigDecimal totalAmountCollected;
    private long totalCampaigns;
    private long activeCampaigns;
    private long completedCampaigns;
    private long totalVolunteers;
    private long activeVolunteers;
    private long totalDonors;
    private long pendingPickups;
    private long completedPickups;
    private long totalPayments;
    private BigDecimal totalPaymentAmount;
    private long goodsDonations;

    public ReportDTO() {
    }

    // Getters and Setters
    public long getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(long totalDonations) {
        this.totalDonations = totalDonations;
    }

    public BigDecimal getTotalAmountCollected() {
        return totalAmountCollected;
    }

    public void setTotalAmountCollected(BigDecimal totalAmountCollected) {
        this.totalAmountCollected = totalAmountCollected;
    }

    public long getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(long totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public long getActiveCampaigns() {
        return activeCampaigns;
    }

    public void setActiveCampaigns(long activeCampaigns) {
        this.activeCampaigns = activeCampaigns;
    }

    public long getCompletedCampaigns() {
        return completedCampaigns;
    }

    public void setCompletedCampaigns(long completedCampaigns) {
        this.completedCampaigns = completedCampaigns;
    }

    public long getTotalVolunteers() {
        return totalVolunteers;
    }

    public void setTotalVolunteers(long totalVolunteers) {
        this.totalVolunteers = totalVolunteers;
    }

    public long getActiveVolunteers() {
        return activeVolunteers;
    }

    public void setActiveVolunteers(long activeVolunteers) {
        this.activeVolunteers = activeVolunteers;
    }

    public long getTotalDonors() {
        return totalDonors;
    }

    public void setTotalDonors(long totalDonors) {
        this.totalDonors = totalDonors;
    }

    public long getPendingPickups() {
        return pendingPickups;
    }

    public void setPendingPickups(long pendingPickups) {
        this.pendingPickups = pendingPickups;
    }

    public long getCompletedPickups() {
        return completedPickups;
    }

    public void setCompletedPickups(long completedPickups) {
        this.completedPickups = completedPickups;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public BigDecimal getTotalPaymentAmount() {
        return totalPaymentAmount;
    }

    public void setTotalPaymentAmount(BigDecimal totalPaymentAmount) {
        this.totalPaymentAmount = totalPaymentAmount;
    }

    public long getGoodsDonations() {
        return goodsDonations;
    }

    public void setGoodsDonations(long goodsDonations) {
        this.goodsDonations = goodsDonations;
    }
}
