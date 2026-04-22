package com.ngo.donation_management.repository;

// repository/DonationReceiptRepository.java

import com.ngo.donation_management.entity.DonationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationReceiptRepository
        extends JpaRepository<DonationReceipt, Integer> {
    List<DonationReceipt> findByDonation_DonationId(
            Integer donationId);
    Optional<DonationReceipt> findFirstByDonation_DonationId(
            Integer donationId);
    Optional<DonationReceipt> findByReceiptNumber(
            String receiptNumber);
}
