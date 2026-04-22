package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationItem;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.DonationItemRepository;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DonationItemService {

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    public DonationItem createDonationItem(DonationItem item) {
        Donation donation = getDonationOrThrow(item);
        ensureDonationAccess(donation);
        item.setDonation(donation);
        DonationItem saved = donationItemRepository.save(item);

        List<PickupRequest> pickups =
                pickupRequestRepository.findByDonation_DonationId(
                        saved.getDonation().getDonationId());

        if (!pickups.isEmpty()) {
            PickupRequest latestPickup = pickups.get(pickups.size() - 1);
            notificationService.notifyVolunteersNewDonationItem(saved, latestPickup);
        }

        return saved;
    }

    public List<DonationItem> getAllDonationItems() {
        return donationItemRepository.findAll().stream()
                .filter(this::canAccessDonationItem)
                .toList();
    }

    public Optional<DonationItem> getDonationItemById(Integer id) {
        return donationItemRepository.findById(id)
                .filter(this::canAccessDonationItem);
    }

    public List<DonationItem> getByDonationId(Integer donationId) {
        return donationItemRepository.findByDonation_DonationId(donationId).stream()
                .filter(this::canAccessDonationItem)
                .toList();
    }

    public List<DonationItem> getByCategory(String category) {
        return donationItemRepository.findByCategory(category).stream()
                .filter(this::canAccessDonationItem)
                .toList();
    }

    public List<DonationItem> searchByItemName(String itemName) {
        return donationItemRepository.findByItemNameContainingIgnoreCase(itemName).stream()
                .filter(this::canAccessDonationItem)
                .toList();
    }

    public DonationItem updateDonationItem(Integer id, DonationItem updatedItem) {
        return donationItemRepository.findById(id).map(item -> {
            ensureDonationItemAccess(item);
            if (updatedItem.getDonation() != null
                    && updatedItem.getDonation().getDonationId() != null) {
                Donation donation = getDonationOrThrow(updatedItem);
                ensureDonationAccess(donation);
                item.setDonation(donation);
            }
            item.setItemName(updatedItem.getItemName());
            item.setCategory(updatedItem.getCategory());
            item.setQuantity(updatedItem.getQuantity());
            item.setDescription(updatedItem.getDescription());
            item.setEstimatedValue(updatedItem.getEstimatedValue());
            return donationItemRepository.save(item);
        }).orElseThrow(() -> new RuntimeException(
                "Donation item not found with id: " + id));
    }

    public void deleteDonationItem(Integer id) {
        DonationItem item = donationItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Donation item not found with id: " + id));
        ensureDonationItemAccess(item);
        donationItemRepository.delete(item);
    }

    private boolean canAccessDonationItem(DonationItem item) {
        try {
            ensureDonationItemAccess(item);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensureDonationItemAccess(DonationItem item) {
        ensureDonationAccess(item != null ? item.getDonation() : null);
    }

    private void ensureDonationAccess(Donation donation) {
        if (donation == null) {
            throw new IllegalArgumentException("Donation is required");
        }

        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer donationUserId = donation.getUser() != null
                    ? donation.getUser().getUserId()
                    : null;
            if (!Objects.equals(currentUser.getUserId(), donationUserId)) {
                throw new AccessDeniedException(
                        "You can only access items for your own donations");
            }
            return;
        }

        Integer ngoId = resolveNgoId(donation);
        if (ngoId != null) {
            accessScopeService.ensureNgoAccess(ngoId);
            return;
        }

        if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new AccessDeniedException(
                    "NGO admins can only access donation items for their NGO");
        }

        throw new AccessDeniedException("Access denied");
    }

    private Donation getDonationOrThrow(DonationItem item) {
        Integer donationId = item != null && item.getDonation() != null
                ? item.getDonation().getDonationId()
                : null;
        if (donationId == null) {
            throw new IllegalArgumentException("Donation is required");
        }

        return donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException(
                        "Donation not found with id: " + donationId));
    }

    private Integer resolveNgoId(Donation donation) {
        if (donation.getCampaign() != null
                && donation.getCampaign().getNgo() != null
                && donation.getCampaign().getNgo().getNgoId() != null) {
            return donation.getCampaign().getNgo().getNgoId();
        }

        return donation.getNgo() != null ? donation.getNgo().getNgoId() : null;
    }
}
