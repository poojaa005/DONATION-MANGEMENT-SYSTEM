package com.ngo.donation_management.entity;
// entity/UrgentNeeds.java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "urgent_needs")
public class UrgentNeeds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "urgent_id")
    private Integer urgentId;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @ManyToOne
    @JoinColumn(name = "ngo_id")
    private Ngo ngo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgent_status", nullable = false)
    private UrgentStatus urgentStatus;

    public enum UrgentStatus {
        open, closed, fulfilled
    }

    // Default Constructor
    public UrgentNeeds() {
    }

    // Parameterized Constructor
    public UrgentNeeds(Integer urgentId, User admin, Ngo ngo,
                       String title, String message,
                       LocalDateTime startTime, LocalDateTime endTime,
                       LocalDateTime createdAt, UrgentStatus urgentStatus) {
        this.urgentId = urgentId;
        this.admin = admin;
        this.ngo = ngo;
        this.title = title;
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.urgentStatus = urgentStatus;
    }

    // Getters and Setters
    public Integer getUrgentId() {
        return urgentId;
    }

    public void setUrgentId(Integer urgentId) {
        this.urgentId = urgentId;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }

    public Ngo getNgo() {
        return ngo;
    }

    public void setNgo(Ngo ngo) {
        this.ngo = ngo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UrgentStatus getUrgentStatus() {
        return urgentStatus;
    }

    public void setUrgentStatus(UrgentStatus urgentStatus) {
        this.urgentStatus = urgentStatus;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
