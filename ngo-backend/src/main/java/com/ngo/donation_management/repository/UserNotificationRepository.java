package com.ngo.donation_management.repository;

import com.ngo.donation_management.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    List<UserNotification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    long countByUser_UserIdAndIsReadFalse(Integer userId);
    Optional<UserNotification> findByUser_UserIdAndEventKey(Integer userId, String eventKey);
    void deleteByUser_UserId(Integer userId);
}
