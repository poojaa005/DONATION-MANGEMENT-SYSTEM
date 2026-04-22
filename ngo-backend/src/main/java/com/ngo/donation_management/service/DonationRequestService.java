package com.ngo.donation_management.service;

// service/DonationRequestService.java
import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.entity.DonationRequest;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.CampaignRepository;
import com.ngo.donation_management.repository.DonationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DonationRequestService {

    @Autowired
    private DonationRequestRepository donationRequestRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    public DonationRequest createDonationRequest(
            DonationRequest request) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (currentUser.getRole() == User.Role.donor) {
            request.setUser(currentUser);
        } else if (!accessScopeService.isAdminLike(currentUser)) {
            throw new IllegalArgumentException("Access denied");
        }

        if (request.getUser() == null || request.getUser().getUserId() == null) {
            throw new IllegalArgumentException("Donation request donor is required");
        }

        if (request.getCampaign() == null || request.getCampaign().getCampaignId() == null) {
            throw new IllegalArgumentException("Campaign is required");
        }

        Campaign campaign = campaignRepository.findById(request.getCampaign().getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campaign not found with id: " + request.getCampaign().getCampaignId()));
        request.setCampaign(campaign);

        if (request.getRequestStatus() == null) {
            request.setRequestStatus(DonationRequest.RequestStatus.pending);
        }

        DonationRequest savedRequest = donationRequestRepository.save(request);
        notificationService.notifyNgoAdminDonationRequestSubmitted(savedRequest);
        notificationService.notifyAppAdminDonationRequestSubmitted(savedRequest);
        return savedRequest;
    }

    public List<DonationRequest> getAllDonationRequests() {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return donationRequestRepository.findByCampaign_Ngo_NgoId(
                    scopedNgoId.get());
        }

        if (accessScopeService.isDonor(currentUser)) {
            return donationRequestRepository.findByUser_UserId(currentUser.getUserId());
        }

        return donationRequestRepository.findAll();
    }

    public Optional<DonationRequest> getDonationRequestById(
            Integer id) {
        return donationRequestRepository.findById(id)
                .filter(this::canAccessDonationRequest);
    }

    public List<DonationRequest> getByUserId(Integer userId) {
        return donationRequestRepository
                .findByUser_UserId(userId).stream()
                .filter(this::canAccessDonationRequest)
                .toList();
    }

    public List<DonationRequest> getByCampaignId(
            Integer campaignId) {
        List<DonationRequest> requests = donationRequestRepository
                .findByCampaign_CampaignId(campaignId);
        return requests.stream()
                .filter(this::canAccessDonationRequest)
                .toList();
    }

    public List<DonationRequest> getByStatus(String status) {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        DonationRequest.RequestStatus requestStatus =
                DonationRequest.RequestStatus.valueOf(status.toLowerCase());
        if (scopedNgoId.isPresent()) {
            return donationRequestRepository
                    .findByRequestStatusAndCampaign_Ngo_NgoId(
                            requestStatus, scopedNgoId.get());
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return donationRequestRepository.findByRequestStatus(requestStatus);
        }

        return donationRequestRepository.findByRequestStatus(requestStatus)
                .stream()
                .filter(this::canAccessDonationRequest)
                .toList();
    }

    public List<DonationRequest> getByDonationType(String type) {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        DonationRequest.DonationType donationType =
                DonationRequest.DonationType.valueOf(type.toLowerCase());
        if (scopedNgoId.isPresent()) {
            return donationRequestRepository
                    .findByDonationTypeAndCampaign_Ngo_NgoId(
                            donationType, scopedNgoId.get());
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return donationRequestRepository.findByDonationType(donationType);
        }

        return donationRequestRepository.findByDonationType(donationType).stream()
                .filter(this::canAccessDonationRequest)
                .toList();
    }

    public DonationRequest updateDonationRequest(
            Integer id, DonationRequest updatedRequest) {
        return donationRequestRepository.findById(id)
                .map(request -> {
                    ensureDonationRequestAccess(request);
                    User currentUser = accessScopeService.requireCurrentUser();
                    if (accessScopeService.isAdminLike(currentUser)) {
                        if (updatedRequest.getUser() != null) {
                            request.setUser(updatedRequest.getUser());
                        }

                        if (updatedRequest.getCampaign() != null
                                && updatedRequest.getCampaign().getCampaignId() != null) {
                            Campaign campaign = campaignRepository.findById(
                                            updatedRequest.getCampaign().getCampaignId())
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Campaign not found with id: "
                                                    + updatedRequest.getCampaign().getCampaignId()));
                            request.setCampaign(campaign);
                        }
                    } else if (request.getRequestStatus() != DonationRequest.RequestStatus.pending) {
                        throw new IllegalArgumentException(
                                "Only pending donation requests can be edited");
                    }

                    request.setDonationType(
                            updatedRequest.getDonationType());
                    request.setAmount(updatedRequest.getAmount());
                    request.setRequestMessage(
                            updatedRequest.getRequestMessage());

                    if (accessScopeService.isAdminLike(currentUser)) {
                        request.setRequestStatus(
                                updatedRequest.getRequestStatus());
                        request.setApprovedBy(updatedRequest.getApprovedBy());
                        request.setApprovedAt(updatedRequest.getApprovedAt());
                    }

                    return donationRequestRepository.save(request);
                }).orElseThrow(() -> new RuntimeException(
                        "Donation request not found with id: " + id));
    }

    public DonationRequest approveRequest(Integer id) {
        return donationRequestRepository.findById(id)
                .map(request -> {
                    User approvedByUser = requireAdminApprover();
                    ensureDonationRequestAccess(request);
                    request.setRequestStatus(
                            DonationRequest.RequestStatus.approved);
                    request.setApprovedBy(approvedByUser);
                    request.setApprovedAt(LocalDateTime.now());
                    request.setRejectionReason(null);
                    DonationRequest savedRequest = donationRequestRepository.save(request);
                    notificationService.notifyDonorDonationRequestApproved(savedRequest);
                    return savedRequest;
                }).orElseThrow(() -> new RuntimeException(
                        "Donation request not found with id: " + id));
    }

    public DonationRequest rejectRequest(Integer id, String rejectionReason) {
        return donationRequestRepository.findById(id)
                .map(request -> {
                    requireAdminApprover();
                    ensureDonationRequestAccess(request);
                    String normalizedReason = normalizeRejectionReason(rejectionReason);
                    request.setRequestStatus(
                            DonationRequest.RequestStatus.rejected);
                    request.setApprovedBy(null);
                    request.setApprovedAt(null);
                    request.setRejectionReason(normalizedReason);
                    DonationRequest savedRequest = donationRequestRepository.save(request);
                    notificationService.notifyDonorDonationRequestRejected(
                            savedRequest, normalizedReason);
                    return savedRequest;
                }).orElseThrow(() -> new RuntimeException(
                        "Donation request not found with id: " + id));
    }

    public void deleteDonationRequest(Integer id) {
        DonationRequest request = donationRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Donation request not found with id: " + id));
        ensureDonationRequestAccess(request);
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isDonor(currentUser)
                && request.getRequestStatus() != DonationRequest.RequestStatus.pending) {
            throw new IllegalArgumentException(
                    "Only pending donation requests can be deleted");
        }
        donationRequestRepository.delete(request);
    }

    private boolean canAccessDonationRequest(DonationRequest request) {
        try {
            ensureDonationRequestAccess(request);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensureDonationRequestAccess(DonationRequest request) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer requestUserId = request != null
                    && request.getUser() != null
                    ? request.getUser().getUserId()
                    : null;
            if (requestUserId == null
                    || !currentUser.getUserId().equals(requestUserId)) {
                throw new AccessDeniedException(
                        "You can only access your own donation requests");
            }
            return;
        }

        if (request != null
                && request.getCampaign() != null
                && request.getCampaign().getNgo() != null) {
            accessScopeService.ensureNgoAccess(
                    request.getCampaign().getNgo().getNgoId());
        } else if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new IllegalArgumentException(
                    "NGO admins can only manage donation requests for their NGO");
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    private User requireAdminApprover() {
        User currentUser = accessScopeService.requireCurrentUser();
        if (!accessScopeService.isAdminLike(currentUser)) {
            throw new IllegalArgumentException(
                    "Only app admin or NGO admin can approve donation requests");
        }
        return currentUser;
    }

    private String normalizeRejectionReason(String rejectionReason) {
        String normalizedReason = rejectionReason == null ? "" : rejectionReason.trim();
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        return normalizedReason;
    }
}
