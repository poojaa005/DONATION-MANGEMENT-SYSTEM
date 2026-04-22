package com.ngo.donation_management.repository;

// repository/NgoRepository.java

import com.ngo.donation_management.entity.Ngo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NgoRepository extends JpaRepository<Ngo, Integer> {
    List<Ngo> findByNgoNameContainingIgnoreCase(String ngoName);
    List<Ngo> findByCityContainingIgnoreCase(String city);
    List<Ngo> findByStateContainingIgnoreCase(String state);
    List<Ngo> findByCityContainingIgnoreCaseOrStateContainingIgnoreCase(
            String city, String state);
}