package com.ngo.donation_management.service;

// service/PickupRequestService.java

import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.TaskAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PickupRequestService {

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    public PickupRequest createPickupRequest(
            PickupRequest pickupRequest) {
        Integer donationId = pickupRequest.getDonation() != null
                ? pickupRequest.getDonation().getDonationId()
                : null;
        if (donationId == null) {
            throw new IllegalArgumentException("Donation is required for pickup scheduling");
        }

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Donation not found with id: " + donationId));
        ensureDonationAccess(donation);
        pickupRequest.setDonation(donation);

        if (pickupRequest.getPickupStatus() == null
                || accessScopeService.isDonor(accessScopeService.requireCurrentUser())) {
            pickupRequest.setPickupStatus(PickupRequest.PickupStatus.awaiting_approval);
        }

        if ((pickupRequest.getDonorPhone() == null || pickupRequest.getDonorPhone().isBlank())
                && donation.getUser() != null) {
            pickupRequest.setDonorPhone(donation.getUser().getPhone());
        }

        PickupRequest savedPickup = pickupRequestRepository.save(pickupRequest);
        notificationService.notifyDonorPickupScheduled(savedPickup);
        notificationService.notifyNgoAdminNewDonation(savedPickup.getDonation());
        notificationService.notifyNgoAdminPickupRequest(savedPickup);
        notificationService.notifyAppAdminDonationActivity(savedPickup.getDonation());
        return savedPickup;
    }

    public List<PickupRequest> getAllPickupRequests() {
        return filterByScope(pickupRequestRepository.findAll());
    }

    public Optional<PickupRequest> getPickupRequestById(
            Integer id) {
        return pickupRequestRepository.findById(id)
                .filter(this::canAccessPickup);
    }

    public List<PickupRequest> getByDonationId(
            Integer donationId) {
        return filterByScope(pickupRequestRepository
                .findByDonation_DonationId(donationId));
    }

    public List<PickupRequest> getByStatus(String status) {
        return filterByScope(pickupRequestRepository.findByPickupStatus(
                PickupRequest.PickupStatus.valueOf(
                        status.toLowerCase())));
    }

    public PickupRequest updatePickupRequest(
            Integer id, PickupRequest updatedPickup) {
        return pickupRequestRepository.findById(id)
                .map(pickup -> {
                    ensurePickupWriteAccess(pickup);
                    User currentUser = accessScopeService.requireCurrentUser();
                    if (accessScopeService.isDonor(currentUser)
                            && pickup.getPickupStatus() != PickupRequest.PickupStatus.awaiting_approval) {
                        throw new IllegalArgumentException(
                                "Only awaiting approval pickups can be edited");
                    }

                    if (updatedPickup.getDonation() != null
                            && updatedPickup.getDonation().getDonationId() != null) {
                        Donation donation = donationRepository.findById(
                                        updatedPickup.getDonation().getDonationId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Donation not found with id: "
                                                + updatedPickup.getDonation().getDonationId()));
                        ensureDonationAccess(donation);
                        pickup.setDonation(donation);
                    }
                    pickup.setDonorAddress(
                            updatedPickup.getDonorAddress());
                    pickup.setDonorPhone(
                            updatedPickup.getDonorPhone());
                    pickup.setPickupDate(updatedPickup.getPickupDate());
                    pickup.setTimeSlot(updatedPickup.getTimeSlot());
                    if (accessScopeService.isAdminLike(currentUser)) {
                        pickup.setPickupStatus(
                                updatedPickup.getPickupStatus());
                    }
                    return pickupRequestRepository.save(pickup);
                }).orElseThrow(() -> new RuntimeException(
                        "Pickup request not found with id: " + id));
    }

    public void deletePickupRequest(Integer id) {
        PickupRequest pickup = pickupRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Pickup request not found with id: " + id));
        ensurePickupWriteAccess(pickup);
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isDonor(currentUser)
                && pickup.getPickupStatus() != PickupRequest.PickupStatus.awaiting_approval) {
            throw new IllegalArgumentException(
                    "Only awaiting approval pickups can be deleted");
        }
        pickupRequestRepository.delete(pickup);
    }

    public PickupRequest approvePickup(Integer id) {
        return pickupRequestRepository.findById(id)
                .map(pickup -> {
                    ensurePickupApprovalAccess(pickup);
                    pickup.setPickupStatus(PickupRequest.PickupStatus.pending);
                    pickup.setRejectionReason(null);

                    Donation donation = pickup.getDonation();
                    if (donation != null) {
                        donation.setDonationStatus(Donation.DonationStatus.approved);
                        donationRepository.save(donation);
                    }

                    PickupRequest savedPickup = pickupRequestRepository.save(pickup);
                    notificationService.notifyDonorPickupApproved(savedPickup);
                    notificationService.notifyVolunteersPickupAvailable(savedPickup);
                    return savedPickup;
                }).orElseThrow(() -> new RuntimeException(
                        "Pickup request not found with id: " + id));
    }

    public PickupRequest rejectPickup(Integer id, String rejectionReason) {
        return pickupRequestRepository.findById(id)
                .map(pickup -> {
                    ensurePickupApprovalAccess(pickup);
                    String normalizedReason = normalizeRejectionReason(rejectionReason);
                    pickup.setPickupStatus(PickupRequest.PickupStatus.cancelled);
                    pickup.setRejectionReason(normalizedReason);

                    Donation donation = pickup.getDonation();
                    if (donation != null) {
                        donation.setDonationStatus(Donation.DonationStatus.cancelled);
                        donationRepository.save(donation);
                    }

                    PickupRequest savedPickup = pickupRequestRepository.save(pickup);
                    notificationService.notifyDonorPickupRejected(savedPickup, normalizedReason);
                    return savedPickup;
                }).orElseThrow(() -> new RuntimeException(
                        "Pickup request not found with id: " + id));
    }

    public long countByStatus(String status) {
        return getByStatus(status).size();
    }

    private List<PickupRequest> filterByScope(List<PickupRequest> pickups) {
        return pickups.stream()
                .filter(this::canAccessPickup)
                .collect(Collectors.toList());
    }

    private void ensurePickupReadAccess(PickupRequest pickup) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer donorUserId = pickup != null
                    && pickup.getDonation() != null
                    && pickup.getDonation().getUser() != null
                    ? pickup.getDonation().getUser().getUserId()
                    : null;
            if (!Objects.equals(currentUser.getUserId(), donorUserId)) {
                throw new AccessDeniedException(
                        "You can only access your own pickups");
            }
            return;
        }

        if (accessScopeService.isVolunteer(currentUser)) {
            boolean assignedToVolunteer = taskAssignmentRepository
                    .findByPickupRequest_PickupId(pickup.getPickupId()).stream()
                    .anyMatch(task -> task.getVolunteer() != null
                            && task.getVolunteer().getUser() != null
                            && Objects.equals(
                                    task.getVolunteer().getUser().getUserId(),
                                    currentUser.getUserId()));
            if (!assignedToVolunteer) {
                throw new AccessDeniedException(
                        "You can only access pickups assigned to you");
            }
            return;
        }

        Integer ngoId = resolvePickupNgoId(pickup);
        if (ngoId != null) {
            accessScopeService.ensureNgoAccess(ngoId);
        } else if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new IllegalArgumentException(
                    "NGO admins can only manage pickups linked to their NGO");
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    private boolean canAccessPickup(PickupRequest pickup) {
        try {
            ensurePickupReadAccess(pickup);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensurePickupWriteAccess(PickupRequest pickup) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isVolunteer(currentUser)) {
            throw new AccessDeniedException("Volunteers cannot modify pickup requests");
        }

        ensurePickupReadAccess(pickup);
    }

    private void ensurePickupApprovalAccess(PickupRequest pickup) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (!accessScopeService.isAdminLike(currentUser)) {
            throw new AccessDeniedException(
                    "Only app admin or NGO admin can approve or reject pickups");
        }

        ensurePickupReadAccess(pickup);
    }

    private void ensureDonationAccess(Donation donation) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer donationUserId = donation != null
                    && donation.getUser() != null
                    ? donation.getUser().getUserId()
                    : null;
            if (!Objects.equals(currentUser.getUserId(), donationUserId)) {
                throw new AccessDeniedException(
                        "You can only create pickups for your own donations");
            }
            return;
        }

        Integer ngoId = resolvePickupNgoIdForDonation(donation);
        if (ngoId != null) {
            accessScopeService.ensureNgoAccess(ngoId);
            return;
        }

        if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new AccessDeniedException(
                    "NGO admins can only manage pickups for their NGO");
        }

        throw new AccessDeniedException("Access denied");
    }

    private Integer resolvePickupNgoId(PickupRequest pickup) {
        Donation donation = pickup != null ? pickup.getDonation() : null;
        return resolvePickupNgoIdForDonation(donation);
    }

    private Integer resolvePickupNgoIdForDonation(Donation donation) {
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

    private String normalizeRejectionReason(String rejectionReason) {
        String normalizedReason = rejectionReason == null ? "" : rejectionReason.trim();
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        return normalizedReason;
    }
}
