package com.ngo.donation_management.service;

// service/DonationService.java

import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.entity.Payment;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.CampaignRepository;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.NgoRepository;
import com.ngo.donation_management.repository.PaymentRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DonationService {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    public Donation createDonation(Donation donation) {
        applyCurrentDonorOwnership(donation);
        normalizeDonationAssociations(donation);
        ensureDonationWriteAccess(donation);
        if (accessScopeService.isDonor(accessScopeService.requireCurrentUser())) {
            donation.setDonationStatus(Donation.DonationStatus.pending);
        }
        return donationRepository.save(donation);
    }

    public List<Donation> getAllDonations() {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return normalizeStatuses(
                    donationRepository.findByScopedNgoId(scopedNgoId.get()));
        }

        Optional<com.ngo.donation_management.entity.User> currentUser =
                accessScopeService.getCurrentUser();
        if (currentUser.isPresent()
                && currentUser.get().getRole() == com.ngo.donation_management.entity.User.Role.donor) {
            return normalizeStatuses(donationRepository.findByUser_UserId(
                    currentUser.get().getUserId()));
        }
        return normalizeStatuses(donationRepository.findAll());
    }

    public Optional<Donation> getDonationById(Integer id) {
        return donationRepository.findById(id)
                .filter(this::canAccessDonation)
                .map(this::normalizeStatus);
    }

    public List<Donation> getByUserId(Integer userId) {
        return normalizeStatuses(filterDonationsByScope(
                donationRepository.findByUser_UserId(userId)));
    }

    public List<Donation> getByCampaignId(Integer campaignId) {
        return normalizeStatuses(filterDonationsByScope(donationRepository
                .findByCampaign_CampaignId(campaignId)));
    }

    public List<Donation> getByStatus(String status) {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        Donation.DonationStatus donationStatus =
                Donation.DonationStatus.valueOf(status.toLowerCase());
        if (scopedNgoId.isPresent()) {
            return normalizeStatuses(donationRepository.findByDonationStatusAndScopedNgoId(
                    donationStatus, scopedNgoId.get()));
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return normalizeStatuses(donationRepository.findByDonationStatus(donationStatus));
        }

        return normalizeStatuses(donationRepository.findByDonationStatus(donationStatus).stream()
                .filter(this::canAccessDonation)
                .toList());
    }

    public List<Donation> getByType(String type) {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        Donation.DonationType donationType =
                Donation.DonationType.valueOf(type.toLowerCase());
        if (scopedNgoId.isPresent()) {
            return normalizeStatuses(donationRepository.findByDonationTypeAndScopedNgoId(
                    donationType, scopedNgoId.get()));
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return normalizeStatuses(donationRepository.findByDonationType(donationType));
        }

        return normalizeStatuses(donationRepository.findByDonationType(donationType).stream()
                .filter(this::canAccessDonation)
                .toList());
    }

    public Donation updateDonation(Integer id,
                                   Donation updatedDonation) {
        return donationRepository.findById(id).map(donation -> {
            ensureDonationAccess(donation);
            User currentUser = accessScopeService.requireCurrentUser();
            if (accessScopeService.isDonor(currentUser)
                    && donation.getDonationStatus() != Donation.DonationStatus.pending) {
                throw new IllegalArgumentException(
                        "Only pending donations can be edited");
            }

            applyCurrentDonorOwnership(updatedDonation);
            normalizeDonationAssociations(updatedDonation);
            ensureDonationWriteAccess(updatedDonation);
            donation.setUser(updatedDonation.getUser());
            donation.setCampaign(updatedDonation.getCampaign());
            donation.setNgo(updatedDonation.getNgo());
            donation.setDonationType(
                    updatedDonation.getDonationType());
            donation.setAmount(updatedDonation.getAmount());

            if (accessScopeService.isAdminLike(currentUser)) {
                donation.setDonationStatus(
                        updatedDonation.getDonationStatus());
            }

            return donationRepository.save(donation);
        }).orElseThrow(() -> new RuntimeException(
                "Donation not found with id: " + id));
    }

    public void deleteDonation(Integer id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Donation not found with id: " + id));
        ensureDonationAccess(donation);
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isDonor(currentUser)
                && donation.getDonationStatus() != Donation.DonationStatus.pending) {
            throw new IllegalArgumentException(
                    "Only pending donations can be deleted");
        }
        donationRepository.delete(donation);
    }

    public BigDecimal getTotalCompletedAmount() {
        return donationRepository.getTotalCompletedAmount();
    }

    public long countByStatus(String status) {
        return getByStatus(status).size();
    }

    @Transactional
    protected List<Donation> normalizeStatuses(List<Donation> donations) {
        return donations.stream()
                .map(this::normalizeStatus)
                .toList();
    }

    @Transactional
    protected Donation normalizeStatus(Donation donation) {
        if (donation == null
                || donation.getDonationId() == null
                || donation.getDonationStatus() == Donation.DonationStatus.completed) {
            return donation;
        }

        boolean shouldMarkCompleted = false;

        if (donation.getDonationType() == Donation.DonationType.monetary) {
            shouldMarkCompleted =
                    paymentRepository.existsByDonation_DonationIdAndPaymentStatus(
                            donation.getDonationId(),
                            Payment.PaymentStatus.success);
        } else if (donation.getDonationType() == Donation.DonationType.goods) {
            shouldMarkCompleted =
                    pickupRequestRepository.existsByDonation_DonationIdAndPickupStatus(
                            donation.getDonationId(),
                            com.ngo.donation_management.entity.PickupRequest.PickupStatus.completed);
        }

        if (shouldMarkCompleted) {
            donation.setDonationStatus(Donation.DonationStatus.completed);
            return donationRepository.save(donation);
        }

        return donation;
    }

    private List<Donation> filterDonationsByScope(List<Donation> donations) {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            Integer ngoId = scopedNgoId.get();
            return donations.stream()
                    .filter(donation -> ngoId.equals(resolveNgoId(donation)))
                    .collect(Collectors.toList());
        }

        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isDonor(currentUser)) {
            return donations.stream()
                    .filter(donation -> donation.getUser() != null
                            && currentUser.getUserId().equals(donation.getUser().getUserId()))
                    .collect(Collectors.toList());
        }

        return donations;
    }

    private boolean canAccessDonation(Donation donation) {
        try {
            ensureDonationAccess(donation);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensureDonationAccess(Donation donation) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer donationUserId = donation != null && donation.getUser() != null
                    ? donation.getUser().getUserId()
                    : null;
            if (donationUserId == null || !currentUser.getUserId().equals(donationUserId)) {
                throw new AccessDeniedException(
                        "You can only access your own donations");
            }
            return;
        }

        Integer ngoId = resolveNgoId(donation);
        if (ngoId != null) {
            accessScopeService.ensureNgoAccess(ngoId);
        } else if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new IllegalArgumentException(
                    "NGO admins can only view donations linked to their NGO");
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void ensureDonationWriteAccess(Donation donation) {
        com.ngo.donation_management.entity.User currentUser =
                accessScopeService.requireCurrentUser();

        if (accessScopeService.isAdminLike(currentUser)) {
            Integer ngoId = resolveNgoId(donation);
            if (ngoId != null) {
                accessScopeService.ensureNgoAccess(ngoId);
            }
            return;
        }

        if (currentUser.getRole() == com.ngo.donation_management.entity.User.Role.donor) {
            Integer donationUserId = donation != null && donation.getUser() != null
                    ? donation.getUser().getUserId()
                    : null;
            if (donationUserId == null || !currentUser.getUserId().equals(donationUserId)) {
                throw new IllegalArgumentException(
                        "You can only submit donations using your own donor account");
            }
            return;
        }

        throw new IllegalArgumentException("Access denied");
    }

    private void applyCurrentDonorOwnership(Donation donation) {
        com.ngo.donation_management.entity.User currentUser =
                accessScopeService.requireCurrentUser();
        if (currentUser.getRole() == com.ngo.donation_management.entity.User.Role.donor) {
            donation.setUser(currentUser);
        }
    }

    private void normalizeDonationAssociations(Donation donation) {
        if (donation == null) {
            return;
        }

        if (donation.getCampaign() != null && donation.getCampaign().getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(donation.getCampaign().getCampaignId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Campaign not found with id: " + donation.getCampaign().getCampaignId()));
            donation.setCampaign(campaign);
            donation.setNgo(campaign.getNgo());
            return;
        }

        donation.setCampaign(null);

        if (donation.getNgo() != null && donation.getNgo().getNgoId() != null) {
            Ngo ngo = ngoRepository.findById(donation.getNgo().getNgoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "NGO not found with id: " + donation.getNgo().getNgoId()));
            donation.setNgo(ngo);
        } else {
            donation.setNgo(null);
        }
    }

    private Integer resolveNgoId(Donation donation) {
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
