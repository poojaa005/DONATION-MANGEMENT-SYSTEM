package com.ngo.donation_management.repository;

import com.ngo.donation_management.entity.DonorLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonorLocationRepository extends JpaRepository<DonorLocation, Integer> {
    Optional<DonorLocation> findByPickupRequest_PickupId(Integer pickupId);
}
