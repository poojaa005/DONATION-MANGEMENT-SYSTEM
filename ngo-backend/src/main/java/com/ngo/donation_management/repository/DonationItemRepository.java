package com.ngo.donation_management.repository;

// repository/DonationItemRepository.java

import com.ngo.donation_management.entity.DonationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationItemRepository
        extends JpaRepository<DonationItem, Integer> {
    List<DonationItem> findByDonation_DonationId(Integer donationId);
    List<DonationItem> findByCategory(String category);
    List<DonationItem> findByItemNameContainingIgnoreCase(
            String itemName);
}