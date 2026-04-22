package com.ngo.donation_management.repository;

// repository/TaskAssignmentRepository.java

import com.ngo.donation_management.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentRepository
        extends JpaRepository<TaskAssignment, Integer> {
    List<TaskAssignment> findByVolunteer_VolunteerId(
            Integer volunteerId);
    List<TaskAssignment> findByTaskStatus(
            TaskAssignment.TaskStatus status);
    List<TaskAssignment> findByPickupRequest_PickupId(
            Integer pickupId);
}