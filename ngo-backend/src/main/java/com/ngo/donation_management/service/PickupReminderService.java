package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.TaskAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PickupReminderService {

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    public void sendUpcomingPickupReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        pickupRequestRepository.findAll().stream()
                .filter(this::isUpcomingPickup)
                .filter(pickup -> pickup.getPickupDate() != null
                        && (pickup.getPickupDate().equals(today)
                        || pickup.getPickupDate().equals(tomorrow)))
                .forEach(notificationService::notifyDonorPickupReminder);

        taskAssignmentRepository.findAll().stream()
                .filter(this::isUpcomingVolunteerTask)
                .filter(task -> {
                    PickupRequest pickup = task.getPickupRequest();
                    return pickup != null
                            && pickup.getPickupDate() != null
                            && (pickup.getPickupDate().equals(today)
                            || pickup.getPickupDate().equals(tomorrow));
                })
                .forEach(notificationService::notifyVolunteerTaskReminder);
    }

    private boolean isUpcomingPickup(PickupRequest pickup) {
        return pickup.getPickupStatus() == PickupRequest.PickupStatus.pending
                || pickup.getPickupStatus() == PickupRequest.PickupStatus.assigned;
    }

    private boolean isUpcomingVolunteerTask(TaskAssignment taskAssignment) {
        return taskAssignment.getTaskStatus() == TaskAssignment.TaskStatus.pending;
    }
}
