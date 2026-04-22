package com.ngo.donation_management.repository;

// repository/UserRepository.java
import com.ngo.donation_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByNameContainingIgnoreCase(String name);
    List<User> findByCity(String city);
    List<User> findByRole(User.Role role);
    List<User> findByNgo_NgoId(Integer ngoId);
    boolean existsByRole(User.Role role);
    boolean existsByRoleAndNgo_NgoId(User.Role role, Integer ngoId);
    long countByRole(User.Role role);
    boolean existsByEmail(String email);
}
