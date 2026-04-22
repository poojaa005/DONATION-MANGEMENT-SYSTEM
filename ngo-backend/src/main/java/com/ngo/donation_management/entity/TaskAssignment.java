package com.ngo.donation_management.entity;

// entity/TaskAssignment.java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_assignment")
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne
    @JoinColumn(name = "pickup_id", nullable = false)
    private PickupRequest pickupRequest;

    @ManyToOne
    @JoinColumn(name = "volunteer_id", nullable = false)
    private Volunteer volunteer;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    private TaskStatus taskStatus;

    public enum TaskStatus {
        pending, in_progress, completed, cancelled
    }

    // Default Constructor
    public TaskAssignment() {
    }

    // Parameterized Constructor
    public TaskAssignment(Integer taskId, PickupRequest pickupRequest,
                          Volunteer volunteer, LocalDateTime assignedDate,
                          TaskStatus taskStatus) {
        this.taskId = taskId;
        this.pickupRequest = pickupRequest;
        this.volunteer = volunteer;
        this.assignedDate = assignedDate;
        this.taskStatus = taskStatus;
    }

    // Getters and Setters
    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public PickupRequest getPickupRequest() {
        return pickupRequest;
    }

    public void setPickupRequest(PickupRequest pickupRequest) {
        this.pickupRequest = pickupRequest;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    @PrePersist
    protected void onCreate() {
        this.assignedDate = LocalDateTime.now();
    }
}