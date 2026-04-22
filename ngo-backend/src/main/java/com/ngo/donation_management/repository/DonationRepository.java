package com.ngo.donation_management.repository;

// repository/DonationRepository.java

import com.ngo.donation_management.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DonationRepository
        extends JpaRepository<Donation, Integer> {
    List<Donation> findByUser_UserId(Integer userId);
    List<Donation> findByCampaign_CampaignId(Integer campaignId);
    List<Donation> findByCampaign_Ngo_NgoId(Integer ngoId);
    @Query("SELECT DISTINCT d FROM Donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId")
    List<Donation> findByScopedNgoId(@Param("ngoId") Integer ngoId);
    List<Donation> findByDonationStatus(
            Donation.DonationStatus status);
    List<Donation> findByDonationStatusAndCampaign_Ngo_NgoId(
            Donation.DonationStatus status, Integer ngoId);
    @Query("SELECT DISTINCT d FROM Donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE d.donationStatus = :status " +
            "AND (cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId)")
    List<Donation> findByDonationStatusAndScopedNgoId(
            @Param("status") Donation.DonationStatus status,
            @Param("ngoId") Integer ngoId);
    List<Donation> findByDonationType(Donation.DonationType type);
    List<Donation> findByDonationTypeAndCampaign_Ngo_NgoId(
            Donation.DonationType type, Integer ngoId);
    @Query("SELECT DISTINCT d FROM Donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE d.donationType = :type " +
            "AND (cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId)")
    List<Donation> findByDonationTypeAndScopedNgoId(
            @Param("type") Donation.DonationType type,
            @Param("ngoId") Integer ngoId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d " +
            "WHERE d.donationStatus = 'completed'")
    BigDecimal getTotalCompletedAmount();

    @Query("SELECT COUNT(DISTINCT d.user.userId) FROM Donation d " +
            "WHERE d.user IS NOT NULL")
    long countDistinctDonors();

    long countByDonationType(Donation.DonationType type);

    long countByDonationStatus(Donation.DonationStatus status);
}
