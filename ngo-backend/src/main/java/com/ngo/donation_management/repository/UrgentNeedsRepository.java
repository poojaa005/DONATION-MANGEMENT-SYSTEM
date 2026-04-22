package com.ngo.donation_management.repository;

// repository/UrgentNeedsRepository.java

import com.ngo.donation_management.entity.UrgentNeeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrgentNeedsRepository
        extends JpaRepository<UrgentNeeds, Integer> {
    List<UrgentNeeds> findByUrgentStatus(
            UrgentNeeds.UrgentStatus status);
    List<UrgentNeeds> findByUrgentStatusAndNgo_NgoId(
            UrgentNeeds.UrgentStatus status, Integer ngoId);
    List<UrgentNeeds> findByTitleContainingIgnoreCase(String title);
    List<UrgentNeeds> findByNgo_NgoId(Integer ngoId);
    List<UrgentNeeds> findByAdmin_UserId(Integer adminId);
}
