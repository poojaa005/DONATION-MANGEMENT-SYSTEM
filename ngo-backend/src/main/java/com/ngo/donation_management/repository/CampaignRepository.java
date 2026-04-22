package com.ngo.donation_management.repository;

// repository/CampaignRepository.java
import com.ngo.donation_management.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository
        extends JpaRepository<Campaign, Integer> {
    List<Campaign> findByCampaignStatus(
            Campaign.CampaignStatus status);
    List<Campaign> findByNgo_NgoId(Integer ngoId);
    List<Campaign> findByAdmin_UserId(Integer adminId);
    List<Campaign> findByTitleContainingIgnoreCase(String title);
    List<Campaign> findByDonationType(Campaign.DonationType type);
    long countByCampaignStatus(Campaign.CampaignStatus status);
}
