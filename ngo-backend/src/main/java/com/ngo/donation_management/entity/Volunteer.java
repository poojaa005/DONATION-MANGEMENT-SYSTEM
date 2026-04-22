package com.ngo.donation_management.entity;

// entity/Volunteer.java

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "volunteer")
public class Volunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "volunteer_id")
    private Integer volunteerId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "ngo_id")
    private Ngo ngo;

    @Enumerated(EnumType.STRING)
    @Column(name = "volunteer_status", nullable = false)
    private VolunteerStatus volunteerStatus;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    public enum VolunteerStatus {
        active, inactive, pending
    }

    // Default Constructor
    public Volunteer() {
    }

    // Parameterized Constructor
    public Volunteer(Integer volunteerId, User user, Ngo ngo,
                     VolunteerStatus volunteerStatus, LocalDate joinedDate) {
        this.volunteerId = volunteerId;
        this.user = user;
        this.ngo = ngo;
        this.volunteerStatus = volunteerStatus;
        this.joinedDate = joinedDate;
    }

    // Getters and Setters
    public Integer getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Integer volunteerId) {
        this.volunteerId = volunteerId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Ngo getNgo() {
        return ngo;
    }

    public void setNgo(Ngo ngo) {
        this.ngo = ngo;
    }

    public VolunteerStatus getVolunteerStatus() {
        return volunteerStatus;
    }

    public void setVolunteerStatus(VolunteerStatus volunteerStatus) {
        this.volunteerStatus = volunteerStatus;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(LocalDate joinedDate) {
        this.joinedDate = joinedDate;
    }
}
