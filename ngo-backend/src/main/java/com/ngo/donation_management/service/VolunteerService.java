package com.ngo.donation_management.service;

// service/VolunteerService.java

import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.UserRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VolunteerService {

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    public Volunteer createVolunteer(Volunteer volunteer) {
        if (volunteer != null) {
            volunteer.setNgo(null);
        }
        ensureVolunteerWriteAccess(volunteer);
        ensureVolunteerAccess(volunteer);
        return volunteerRepository.save(volunteer);
    }

    public List<Volunteer> getAllVolunteers() {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isVolunteer(currentUser)) {
            return volunteerRepository.findByUser_UserId(currentUser.getUserId())
                    .map(List::of)
                    .orElseGet(List::of);
        }

        return volunteerRepository.findAll();
    }

    public Optional<Volunteer> getVolunteerById(Integer id) {
        return volunteerRepository.findById(id)
                .filter(this::canAccessVolunteer);
    }

    public List<Volunteer> getByStatus(String status) {
        List<Volunteer> volunteers = volunteerRepository.findByVolunteerStatus(
                Volunteer.VolunteerStatus.valueOf(status.toLowerCase()));
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isVolunteer(currentUser)) {
            return volunteers.stream()
                    .filter(this::canAccessVolunteer)
                    .collect(Collectors.toList());
        }

        return volunteers;
    }

    public List<Volunteer> getByNgoId(Integer ngoId) {
        if (accessScopeService.isVolunteer(accessScopeService.requireCurrentUser())) {
            throw new AccessDeniedException(
                    "Volunteers cannot browse other volunteer directories");
        }
        return volunteerRepository.findAll();
    }

    public Optional<Volunteer> getByUserId(Integer userId) {
        Optional<Volunteer> existingVolunteer =
                volunteerRepository.findByUser_UserId(userId);

        if (existingVolunteer.isPresent()) {
            return existingVolunteer.filter(this::canAccessVolunteer);
        }

        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent() && user.get().getRole() == User.Role.volunteer) {
            Volunteer volunteer = ensureVolunteerProfile(user.get());
            if (canAccessVolunteer(volunteer)) {
                return Optional.of(volunteer);
            }
        }

        return Optional.empty();
    }

    public Volunteer updateVolunteer(Integer id,
                                     Volunteer updatedVolunteer) {
        return volunteerRepository.findById(id).map(vol -> {
            ensureVolunteerWriteAccess(vol);
            ensureVolunteerAccess(vol);
            ensureVolunteerAccess(updatedVolunteer);
            vol.setUser(updatedVolunteer.getUser());
            vol.setNgo(null);
            vol.setVolunteerStatus(
                    updatedVolunteer.getVolunteerStatus());
            vol.setJoinedDate(updatedVolunteer.getJoinedDate());
            return volunteerRepository.save(vol);
        }).orElseThrow(() -> new RuntimeException(
                "Volunteer not found with id: " + id));
    }

    public void deleteVolunteer(Integer id) {
        Volunteer volunteer = volunteerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Volunteer not found with id: " + id));
        ensureVolunteerWriteAccess(volunteer);
        ensureVolunteerAccess(volunteer);
        volunteerRepository.delete(volunteer);
    }

    public long countActiveVolunteers() {
        return getByStatus("active").size();
    }

    public Volunteer ensureVolunteerProfile(User user) {
        return volunteerRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Volunteer volunteer = new Volunteer();
                    volunteer.setUser(user);
                    volunteer.setNgo(null);
                    volunteer.setVolunteerStatus(
                            Volunteer.VolunteerStatus.active);
                    volunteer.setJoinedDate(LocalDate.now());
                    return volunteerRepository.save(volunteer);
                });
    }

    private boolean canAccessVolunteer(Volunteer volunteer) {
        try {
            ensureVolunteerAccess(volunteer);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensureVolunteerAccess(Volunteer volunteer) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isNgoAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isVolunteer(currentUser)) {
            Integer volunteerUserId = volunteer != null
                    && volunteer.getUser() != null
                    ? volunteer.getUser().getUserId()
                    : null;
            if (!Objects.equals(currentUser.getUserId(), volunteerUserId)) {
                throw new AccessDeniedException(
                        "You can only access your own volunteer profile");
            }
            return;
        }

        throw new AccessDeniedException("Access denied");
    }

    private void ensureVolunteerWriteAccess(Volunteer volunteer) {
        if (!accessScopeService.isAdminLike(accessScopeService.requireCurrentUser())) {
            throw new AccessDeniedException(
                    "Only app admin or NGO admin can manage volunteer records");
        }

        ensureVolunteerAccess(volunteer);
    }
}
