package com.ngo.donation_management.service;

// service/UserService.java

import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationRequest;
import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.UrgentNeeds;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.CampaignRepository;
import com.ngo.donation_management.repository.DonationItemRepository;
import com.ngo.donation_management.repository.DonationReceiptRepository;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.DonationRequestRepository;
import com.ngo.donation_management.repository.DonorLocationRepository;
import com.ngo.donation_management.repository.NgoRepository;
import com.ngo.donation_management.repository.PaymentRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.TaskAssignmentRepository;
import com.ngo.donation_management.repository.UrgentNeedsRepository;
import com.ngo.donation_management.repository.UserRepository;
import com.ngo.donation_management.repository.UserNotificationRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private DonationRequestRepository donationRequestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private DonationReceiptRepository donationReceiptRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private DonorLocationRepository donorLocationRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UrgentNeedsRepository urgentNeedsRepository;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private NotificationService notificationService;

    // Create
    public User createUser(User user) {
        String rawPassword = user.getPassword();
        normalizeAndValidateUser(null, user);
        if (user.getRole() == User.Role.admin
                || user.getRole() == User.Role.ngo_admin) {
            accessScopeService.ensureAppAdmin();
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == User.Role.volunteer) {
            volunteerService.ensureVolunteerProfile(savedUser);
            notificationService.notifyVolunteerRegistrationSuccess(savedUser);
        }

        if (savedUser.getRole() == User.Role.ngo_admin) {
            notificationService.notifyNgoAdminAssigned(savedUser, rawPassword, true);
        }

        if (savedUser.getRole() == User.Role.donor) {
            notificationService.notifyDonorRegistrationSuccess(savedUser);
        }

        return savedUser;
    }

    // Read all
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Read by ID
    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }

    // Read by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getCurrentUserProfile() {
        Integer currentUserId = accessScopeService.requireCurrentUser().getUserId();
        return userRepository.findById(currentUserId).orElseThrow(() ->
                new RuntimeException("User not found with id: " + currentUserId));
    }

    // Search by name
    public List<User> searchByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    // Search by city
    public List<User> searchByCity(String city) {
        return userRepository.findByCity(city);
    }

    // Search by role
    public List<User> searchByRole(String role) {
        return userRepository.findByRole(
                User.Role.valueOf(role.toLowerCase()));
    }

    // Update
    public User updateUser(Integer id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            accessScopeService.ensureAppAdmin();
            String rawPassword = updatedUser.getPassword();
            normalizeAndValidateUser(id, updatedUser);
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            user.setPhone(updatedUser.getPhone());
            if (updatedUser.getPassword() != null
                    && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(
                        passwordEncoder.encode(
                                updatedUser.getPassword()));
            }
            user.setAddress(updatedUser.getAddress());
            user.setCity(updatedUser.getCity());
            user.setNgo(updatedUser.getNgo());
            user.setRole(updatedUser.getRole());
            User savedUser = userRepository.save(user);

            if (savedUser.getRole() == User.Role.volunteer) {
                volunteerService.ensureVolunteerProfile(savedUser);
            }

            if (savedUser.getRole() == User.Role.ngo_admin) {
                notificationService.notifyNgoAdminAssigned(savedUser, rawPassword, true);
            }

            return savedUser;
        }).orElseThrow(() -> new RuntimeException(
                "User not found with id: " + id));
    }

    public User updateCurrentUser(User updatedUser) {
        User currentUser = getCurrentUserProfile();

        if (updatedUser.getName() != null) {
            currentUser.setName(updatedUser.getName().trim());
        }

        if (updatedUser.getPhone() != null) {
            currentUser.setPhone(normalizeNullableText(updatedUser.getPhone()));
        }

        if (updatedUser.getAddress() != null) {
            currentUser.setAddress(normalizeNullableText(updatedUser.getAddress()));
        }

        if (updatedUser.getCity() != null) {
            currentUser.setCity(normalizeNullableText(updatedUser.getCity()));
        }

        if (updatedUser.getPassword() != null
                && !updatedUser.getPassword().trim().isEmpty()) {
            currentUser.setPassword(passwordEncoder.encode(updatedUser.getPassword().trim()));
        }

        return userRepository.save(currentUser);
    }

    // Delete
    @Transactional
    public void deleteUser(Integer id) {
        accessScopeService.ensureAppAdmin();
        User user = userRepository.findById(id).orElseThrow(() ->
                new RuntimeException("User not found with id: " + id));

        List<Campaign> managedCampaigns =
                campaignRepository.findByAdmin_UserId(id);
        List<UrgentNeeds> urgentNeeds =
                urgentNeedsRepository.findByAdmin_UserId(id);

        if (!managedCampaigns.isEmpty() || !urgentNeeds.isEmpty()) {
            User replacementAdmin = findReplacementAdmin(id);

            if (replacementAdmin == null) {
                throw new IllegalStateException(
                        "Create another administrator account before deleting this user.");
            }

            managedCampaigns.forEach(campaign ->
                    campaign.setAdmin(replacementAdmin));
            campaignRepository.saveAll(managedCampaigns);

            urgentNeeds.forEach(need ->
                    need.setAdmin(replacementAdmin));
            urgentNeedsRepository.saveAll(urgentNeeds);
        }

        List<DonationRequest> approvedRequests =
                donationRequestRepository.findByApprovedBy_UserId(id);
        if (!approvedRequests.isEmpty()) {
            approvedRequests.forEach(request -> {
                request.setApprovedBy(null);
                request.setApprovedAt(null);
            });
            donationRequestRepository.saveAll(approvedRequests);
        }

        List<DonationRequest> requestedDonations =
                donationRequestRepository.findByUser_UserId(id);
        if (!requestedDonations.isEmpty()) {
            donationRequestRepository.deleteAll(requestedDonations);
        }

        volunteerRepository.findByUser_UserId(id).ifPresent(this::deleteVolunteerData);

        List<Donation> donations = donationRepository.findByUser_UserId(id);
        for (Donation donation : donations) {
            deleteDonationData(donation);
        }

        userNotificationRepository.deleteByUser_UserId(id);
        userRepository.delete(user);
    }

    private void deleteVolunteerData(Volunteer volunteer) {
        taskAssignmentRepository.deleteAll(
                taskAssignmentRepository.findByVolunteer_VolunteerId(
                        volunteer.getVolunteerId()));
        volunteerRepository.delete(volunteer);
    }

    private void deleteDonationData(Donation donation) {
        List<PickupRequest> pickups =
                pickupRequestRepository.findByDonation_DonationId(
                        donation.getDonationId());

        for (PickupRequest pickup : pickups) {
            taskAssignmentRepository.deleteAll(
                    taskAssignmentRepository.findByPickupRequest_PickupId(
                            pickup.getPickupId()));
            donorLocationRepository.findByPickupRequest_PickupId(
                    pickup.getPickupId()).ifPresent(
                            donorLocationRepository::delete);
            pickupRequestRepository.delete(pickup);
        }

        donationReceiptRepository.deleteAll(
                donationReceiptRepository.findByDonation_DonationId(
                        donation.getDonationId()));
        paymentRepository.deleteAll(
                paymentRepository.findByDonation_DonationId(
                        donation.getDonationId()));
        donationItemRepository.deleteAll(
                donationItemRepository.findByDonation_DonationId(
                        donation.getDonationId()));
        donationRepository.delete(donation);
    }

    private User findReplacementAdmin(Integer deletedUserId) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getName() != null
                && !authentication.getName().isBlank()) {
            Optional<User> currentUser =
                    userRepository.findByEmail(authentication.getName());
            if (currentUser.isPresent()
                    && !currentUser.get().getUserId().equals(deletedUserId)
                    && isAdminRole(currentUser.get())) {
                return currentUser.get();
            }
        }

        List<User> admins = userRepository.findByRole(User.Role.admin);
        for (User admin : admins) {
            if (!admin.getUserId().equals(deletedUserId)) {
                return admin;
            }
        }

        List<User> ngoAdmins = userRepository.findByRole(User.Role.ngo_admin);
        for (User ngoAdmin : ngoAdmins) {
            if (!ngoAdmin.getUserId().equals(deletedUserId)) {
                return ngoAdmin;
            }
        }

        return null;
    }

    private boolean isAdminRole(User user) {
        return user.getRole() == User.Role.admin
                || user.getRole() == User.Role.ngo_admin;
    }

    private void normalizeAndValidateUser(Integer existingUserId, User user) {
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim());
        }

        if (user.getName() != null) {
            user.setName(user.getName().trim());
        }

        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
        }

        if (user.getAddress() != null) {
            user.setAddress(user.getAddress().trim());
        }

        if (user.getCity() != null) {
            user.setCity(user.getCity().trim());
        }

        Ngo assignedNgo = null;
        if (user.getNgo() != null && user.getNgo().getNgoId() != null) {
            Integer ngoId = user.getNgo().getNgoId();
            assignedNgo = ngoRepository.findById(ngoId).orElseThrow(() ->
                    new IllegalArgumentException(
                            "NGO not found with id: " + ngoId));
        }

        if (user.getRole() == User.Role.admin) {
            user.setNgo(null);
            long adminCount = userRepository.countByRole(User.Role.admin);
            if (adminCount > 0 && !isExistingRole(existingUserId, User.Role.admin)) {
                throw new IllegalArgumentException(
                        "Only one app admin account is allowed");
            }
            return;
        }

        if (user.getRole() == User.Role.ngo_admin) {
            if (assignedNgo == null) {
                throw new IllegalArgumentException(
                        "NGO admin must be assigned to an NGO");
            }

            final Integer assignedNgoId = assignedNgo.getNgoId();
            List<User> ngoAdmins = userRepository.findByRole(User.Role.ngo_admin);
            boolean ngoAdminExists = ngoAdmins.stream().anyMatch(existingUser ->
                    existingUser.getNgo() != null
                            && existingUser.getNgo().getNgoId() != null
                            && existingUser.getNgo().getNgoId().equals(
                                    assignedNgoId)
                            && !existingUser.getUserId().equals(existingUserId));
            if (ngoAdminExists) {
                throw new IllegalArgumentException(
                        "This NGO already has an assigned NGO admin");
            }

            user.setNgo(assignedNgo);
            return;
        }

        user.setNgo(null);
    }

    private boolean isExistingRole(Integer existingUserId, User.Role role) {
        if (existingUserId == null) {
            return false;
        }

        return userRepository.findById(existingUserId)
                .map(existingUser -> existingUser.getRole() == role)
                .orElse(false);
    }

    // Check if email exists
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
