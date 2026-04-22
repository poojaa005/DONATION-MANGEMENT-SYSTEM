package com.ngo.donation_management.service;

// service/TaskAssignmentService.java
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.TaskAssignmentRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskAssignmentService {

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    @Autowired
    private AccessScopeService accessScopeService;

    /**
     * Creates a task (admin assigns) and notifies volunteer + donor.
     */
    public TaskAssignment createTask(TaskAssignment task) {
        PickupRequest pickup = pickupRequestRepository.findById(
                        task.getPickupRequest().getPickupId())
                .orElseThrow(() -> new RuntimeException(
                        "Pickup not found with id: " + task.getPickupRequest().getPickupId()));
        validatePickupAssignable(pickup);

        List<TaskAssignment> existingTasks = taskAssignmentRepository
                .findByPickupRequest_PickupId(pickup.getPickupId());
        boolean hasActiveTask = existingTasks.stream().anyMatch(existingTask ->
                existingTask.getTaskStatus() != TaskAssignment.TaskStatus.cancelled
                        && existingTask.getTaskStatus() != TaskAssignment.TaskStatus.completed);
        if (hasActiveTask) {
            throw new IllegalStateException(
                    "This pickup already has an active volunteer task");
        }

        Volunteer requestedVolunteer = task.getVolunteer();
        if (requestedVolunteer == null || requestedVolunteer.getVolunteerId() == null) {
            throw new IllegalArgumentException("Volunteer is required");
        }

        Volunteer volunteer = volunteerRepository.findById(
                        requestedVolunteer.getVolunteerId())
                .orElseThrow(() -> new RuntimeException(
                        "Volunteer not found with id: " + requestedVolunteer.getVolunteerId()));

        User currentUser = accessScopeService.requireCurrentUser();
        if (currentUser.getRole() == User.Role.volunteer) {
            ensureVolunteerOwnsTask(volunteer, currentUser);
        } else {
            ensurePickupAccess(pickup);
        }

        task.setPickupRequest(pickup);
        task.setVolunteer(volunteer);
        TaskAssignment saved = taskAssignmentRepository.save(task);

        // Update pickup status to assigned
        if (pickup.getPickupStatus() == PickupRequest.PickupStatus.pending) {
            pickup.setPickupStatus(PickupRequest.PickupStatus.assigned);
            pickupRequestRepository.save(pickup);
        }

        // Notify volunteer & donor
        notificationService.notifyVolunteerTaskAssigned(saved);
        notificationService.notifyDonorPickupConfirmed(saved);
        notificationService.notifyNgoAdminVolunteerTaskAccepted(saved);
        notificationService.notifyAppAdminVolunteerTaskAccepted(saved);

        return saved;
    }

    public List<TaskAssignment> getAllTasks() {
        return filterTasksByScope(taskAssignmentRepository.findAll());
    }

    public Optional<TaskAssignment> getTaskById(Integer id) {
        return taskAssignmentRepository.findById(id)
                .filter(this::canAccessTask);
    }

    public List<TaskAssignment> getByVolunteerId(
            Integer volunteerId) {
        return filterTasksByScope(taskAssignmentRepository
                .findByVolunteer_VolunteerId(volunteerId));
    }

    public List<TaskAssignment> getByStatus(String status) {
        return filterTasksByScope(taskAssignmentRepository.findByTaskStatus(
                TaskAssignment.TaskStatus.valueOf(
                        status.toLowerCase())));
    }

    public List<TaskAssignment> getByPickupId(Integer pickupId) {
        return filterTasksByScope(taskAssignmentRepository
                .findByPickupRequest_PickupId(pickupId));
    }

    public TaskAssignment updateTask(Integer id,
                                     TaskAssignment updatedTask) {
        return taskAssignmentRepository.findById(id)
                .map(task -> {
                    ensureTaskAccess(task);
                    task.setPickupRequest(updatedTask.getPickupRequest());
                    task.setVolunteer(updatedTask.getVolunteer());
                    task.setTaskStatus(updatedTask.getTaskStatus());
                    return taskAssignmentRepository.save(task);
                }).orElseThrow(() -> new RuntimeException(
                        "Task not found with id: " + id));
    }

    @Transactional
    public TaskAssignment updateTaskStatus(Integer id,
                                           String status) {
        return taskAssignmentRepository.findById(id)
                .map(task -> {
                    ensureTaskAccess(task);
                    TaskAssignment.TaskStatus nextStatus =
                            TaskAssignment.TaskStatus.valueOf(
                                    status.toLowerCase());
                    if (task.getTaskStatus() == nextStatus) {
                        return task;
                    }

                    task.setTaskStatus(nextStatus);

                    PickupRequest pickup = task.getPickupRequest();
                    if (pickup != null && nextStatus == TaskAssignment.TaskStatus.completed) {
                        pickup.setPickupStatus(PickupRequest.PickupStatus.completed);
                        pickupRequestRepository.save(pickup);

                        Donation donation = pickup.getDonation();
                        if (donation != null) {
                            donation.setDonationStatus(Donation.DonationStatus.completed);
                            donationRepository.save(donation);
                        }
                    }

                    TaskAssignment savedTask = taskAssignmentRepository.save(task);

                    if (pickup != null && nextStatus == TaskAssignment.TaskStatus.in_progress) {
                        notificationService.notifyVolunteerTaskStarted(savedTask);
                        notificationService.notifyDonorPickupInProgress(savedTask);
                        notificationService.notifyNgoAdminVolunteerTaskUpdate(savedTask);
                    }

                    if (pickup != null && nextStatus == TaskAssignment.TaskStatus.completed) {
                        notificationService.notifyVolunteerTaskCompleted(savedTask);
                        notificationService.notifyDonorPickupCompleted(savedTask);
                        notificationService.notifyNgoAdminVolunteerTaskUpdate(savedTask);
                    }

                    return savedTask;
                }).orElseThrow(() -> new RuntimeException(
                        "Task not found with id: " + id));
    }

    public void deleteTask(Integer id) {
        TaskAssignment task = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Task not found with id: " + id));
        ensureTaskAccess(task);
        taskAssignmentRepository.delete(task);
    }

    private List<TaskAssignment> filterTasksByScope(List<TaskAssignment> tasks) {
        return tasks.stream()
                .filter(this::canAccessTask)
                .collect(Collectors.toList());
    }

    private boolean canAccessTask(TaskAssignment task) {
        try {
            ensureTaskAccess(task);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensureTaskAccess(TaskAssignment task) {
        if (task == null || task.getPickupRequest() == null) {
            throw new IllegalArgumentException("Task pickup is required");
        }

        User currentUser = accessScopeService.requireCurrentUser();
        if (currentUser.getRole() == User.Role.volunteer) {
            Volunteer volunteer = task.getVolunteer();
            ensureVolunteerOwnsTask(volunteer, currentUser);
            return;
        }

        ensurePickupAccess(task.getPickupRequest());
    }

    private void ensurePickupAccess(PickupRequest pickup) {
        Integer pickupNgoId = resolvePickupNgoId(pickup);
        if (pickupNgoId != null) {
            accessScopeService.ensureNgoAccess(pickupNgoId);
        } else if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new IllegalArgumentException(
                    "NGO admins can only manage tasks linked to their NGO");
        }
    }

    private void validatePickupAssignable(PickupRequest pickup) {
        if (pickup.getPickupStatus() != PickupRequest.PickupStatus.pending
                && pickup.getPickupStatus() != PickupRequest.PickupStatus.assigned) {
            throw new IllegalStateException(
                    "Only pending or assigned pickups can have volunteer tasks");
        }
    }

    private void ensureVolunteerOwnsTask(Volunteer volunteer, User currentUser) {
        if (volunteer == null
                || volunteer.getUser() == null
                || currentUser == null
                || !currentUser.getUserId().equals(volunteer.getUser().getUserId())) {
            throw new IllegalArgumentException(
                    "You can only manage tasks for your own volunteer account");
        }
    }

    private Integer resolvePickupNgoId(PickupRequest pickup) {
        Donation donation = pickup != null ? pickup.getDonation() : null;
        if (donation == null) {
            return null;
        }

        if (donation.getCampaign() != null
                && donation.getCampaign().getNgo() != null
                && donation.getCampaign().getNgo().getNgoId() != null) {
            return donation.getCampaign().getNgo().getNgoId();
        }

        return donation.getNgo() != null ? donation.getNgo().getNgoId() : null;
    }
}
