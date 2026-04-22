package com.ngo.donation_management.repository;

// repository/VolunteerRepository.java
import com.ngo.donation_management.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerRepository
        extends JpaRepository<Volunteer, Integer> {
    List<Volunteer> findByVolunteerStatus(
            Volunteer.VolunteerStatus status);
    List<Volunteer> findByNgo_NgoId(Integer ngoId);
    Optional<Volunteer> findByUser_UserId(Integer userId);
    long countByVolunteerStatus(Volunteer.VolunteerStatus status);
}