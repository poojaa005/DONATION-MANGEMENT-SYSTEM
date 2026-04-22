package com.ngo.donation_management.repository;

// repository/PickupRequestRepository.java

import com.ngo.donation_management.entity.PickupRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupRequestRepository
        extends JpaRepository<PickupRequest, Integer> {
    List<PickupRequest> findByDonation_DonationId(Integer donationId);
    boolean existsByDonation_DonationIdAndPickupStatus(
            Integer donationId, PickupRequest.PickupStatus status);
    List<PickupRequest> findByPickupStatus(
            PickupRequest.PickupStatus status);
    long countByPickupStatus(PickupRequest.PickupStatus status);
}
