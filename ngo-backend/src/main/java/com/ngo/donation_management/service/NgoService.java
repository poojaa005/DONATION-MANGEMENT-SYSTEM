package com.ngo.donation_management.service;

// service/NgoService.java

import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.NgoRepository;
import com.ngo.donation_management.repository.TaskAssignmentRepository;
import com.ngo.donation_management.repository.UrgentNeedsRepository;
import com.ngo.donation_management.repository.UserRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class NgoService {

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrgentNeedsRepository urgentNeedsRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    private NotificationService notificationService;

    // Create
    public Ngo createNgo(Ngo ngo) {
        accessScopeService.ensureAppAdmin();
        Ngo savedNgo = ngoRepository.save(ngo);
        User currentUser = accessScopeService.requireCurrentUser();
        notificationService.notifyNgoCreated(savedNgo, currentUser);
        return savedNgo;
    }

    // Read all
    public List<Ngo> getAllNgos() {
        return ngoRepository.findAll();
    }

    // Read by ID
    public Optional<Ngo> getNgoById(Integer id) {
        return ngoRepository.findById(id);
    }

    // Search by name
    public List<Ngo> searchByName(String name) {
        return ngoRepository.findByNgoNameContainingIgnoreCase(name);
    }

    // Search by city
    public List<Ngo> searchByCity(String city) {
        return ngoRepository.findByCityContainingIgnoreCase(city);
    }

    // Search by state
    public List<Ngo> searchByState(String state) {
        return ngoRepository.findByStateContainingIgnoreCase(state);
    }

    // Search nearby (by city or state)
    public List<Ngo> searchNearby(String location) {
        return ngoRepository
                .findByCityContainingIgnoreCaseOrStateContainingIgnoreCase(
                        location, location);
    }

    // Update
    public Ngo updateNgo(Integer id, Ngo updatedNgo) {
        return ngoRepository.findById(id).map(ngo -> {
            ngo.setNgoName(updatedNgo.getNgoName());
            ngo.setAddress(updatedNgo.getAddress());
            ngo.setCity(updatedNgo.getCity());
            ngo.setState(updatedNgo.getState());
            ngo.setPhone(updatedNgo.getPhone());
            ngo.setEmail(updatedNgo.getEmail());
            ngo.setDescription(updatedNgo.getDescription());
            return ngoRepository.save(ngo);
        }).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "NGO not found with id: " + id));
    }

    // Delete
    @Transactional
    public void deleteNgo(Integer id) {
        accessScopeService.ensureAppAdmin();
        Ngo ngo = ngoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found with id: " + id));

        campaignService.getByNgoId(id).forEach(campaign ->
                campaignService.deleteCampaign(campaign.getCampaignId()));

        List<Volunteer> volunteers =
                volunteerRepository.findByNgo_NgoId(id);
        for (Volunteer volunteer : volunteers) {
            taskAssignmentRepository.deleteAll(
                    taskAssignmentRepository.findByVolunteer_VolunteerId(
                            volunteer.getVolunteerId()));
            volunteerRepository.delete(volunteer);
        }

        urgentNeedsRepository.deleteAll(
                urgentNeedsRepository.findByNgo_NgoId(id));

        List<User> assignedUsers = userRepository.findByNgo_NgoId(id);
        assignedUsers.forEach(user -> {
            user.setNgo(null);
            if (user.getRole() == User.Role.ngo_admin) {
                user.setRole(User.Role.donor);
            }
        });
        userRepository.saveAll(assignedUsers);

        ngoRepository.delete(ngo);
    }
}
