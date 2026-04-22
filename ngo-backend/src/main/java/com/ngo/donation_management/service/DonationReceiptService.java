package com.ngo.donation_management.service;

// service/DonationReceiptService.java
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationReceipt;
import com.ngo.donation_management.entity.Payment;
import com.ngo.donation_management.repository.DonationReceiptRepository;
import com.ngo.donation_management.repository.DonationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DonationReceiptService {

    @Autowired
    private DonationReceiptRepository donationReceiptRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    public DonationReceipt createReceipt(DonationReceipt receipt) {
        Donation donation = getReceiptDonationOrThrow(receipt);
        ensureReceiptWriteAccess(donation);
        receipt.setDonation(donation);
        return donationReceiptRepository.save(receipt);
    }

    public DonationReceipt createOrGetReceipt(Donation donation,
                                              Payment payment) {
        return donationReceiptRepository
                .findFirstByDonation_DonationId(
                        donation.getDonationId())
                .map(existingReceipt -> {
                    if (existingReceipt.getCertificateUrl() == null
                            || existingReceipt.getCertificateUrl().isBlank()) {
                        existingReceipt.setCertificateUrl(
                                buildReceiptUrl(donation.getDonationId()));
                    }
                    return donationReceiptRepository.save(existingReceipt);
                })
                .orElseGet(() -> {
                    DonationReceipt receipt = new DonationReceipt();
                    receipt.setDonation(donation);
                    receipt.setReceiptNumber(
                            buildReceiptNumber(donation, payment));
                    receipt.setIssuedDate(LocalDateTime.now());
                    receipt.setCertificateUrl(
                            buildReceiptUrl(donation.getDonationId()));
                    return donationReceiptRepository.save(receipt);
                });
    }

    public List<DonationReceipt> getAllReceipts() {
        return donationReceiptRepository.findAll().stream()
                .filter(this::canAccessReceipt)
                .toList();
    }

    public Optional<DonationReceipt> getReceiptById(Integer id) {
        return donationReceiptRepository.findById(id)
                .filter(this::canAccessReceipt);
    }

    public List<DonationReceipt> getByDonationId(
            Integer donationId) {
        return donationReceiptRepository
                .findByDonation_DonationId(donationId).stream()
                .filter(this::canAccessReceipt)
                .toList();
    }

    public Optional<DonationReceipt> getByReceiptNumber(
            String receiptNumber) {
        return donationReceiptRepository
                .findByReceiptNumber(receiptNumber)
                .filter(this::canAccessReceipt);
    }

    public DonationReceipt updateReceipt(Integer id,
                                         DonationReceipt updated) {
        return donationReceiptRepository.findById(id)
                .map(receipt -> {
                    ensureReceiptWriteAccess(receipt.getDonation());
                    if (updated.getDonation() != null) {
                        Donation donation = getReceiptDonationOrThrow(updated);
                        ensureReceiptWriteAccess(donation);
                        receipt.setDonation(donation);
                    }
                    receipt.setReceiptNumber(updated.getReceiptNumber());
                    receipt.setCertificateUrl(updated.getCertificateUrl());
                    return donationReceiptRepository.save(receipt);
                }).orElseThrow(() -> new RuntimeException(
                        "Receipt not found with id: " + id));
    }

    public void deleteReceipt(Integer id) {
        DonationReceipt receipt = donationReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Receipt not found with id: " + id));
        ensureReceiptWriteAccess(receipt.getDonation());
        donationReceiptRepository.delete(receipt);
    }

    private String buildReceiptNumber(Donation donation,
                                      Payment payment) {
        return "RCPT-"
                + donation.getDonationId()
                + "-"
                + payment.getPaymentId();
    }

    private String buildReceiptUrl(Integer donationId) {
        return "/donations/receipt/" + donationId;
    }

    private boolean canAccessReceipt(DonationReceipt receipt) {
        try {
            ensureReceiptReadAccess(receipt);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensureReceiptReadAccess(DonationReceipt receipt) {
        ensureDonationReadAccess(getReceiptDonationOrThrow(receipt));
    }

    private void ensureReceiptWriteAccess(Donation donation) {
        if (!accessScopeService.isAdminLike(accessScopeService.requireCurrentUser())) {
            throw new AccessDeniedException("Only admins can manage receipts");
        }

        ensureDonationReadAccess(donation);
    }

    private void ensureDonationReadAccess(Donation donation) {
        if (donation == null) {
            throw new IllegalArgumentException("Donation is required");
        }

        var currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        Integer donorUserId = donation.getUser() != null
                ? donation.getUser().getUserId()
                : null;
        if (accessScopeService.isDonor(currentUser)) {
            if (donorUserId == null
                    || !currentUser.getUserId().equals(donorUserId)) {
                throw new AccessDeniedException(
                        "You can only access your own receipts");
            }
            return;
        }

        Integer ngoId = resolveDonationNgoId(donation);
        if (ngoId != null) {
            accessScopeService.ensureNgoAccess(ngoId);
            return;
        }

        if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new AccessDeniedException(
                    "NGO admins can only access receipts for their NGO");
        }

        throw new AccessDeniedException("Access denied");
    }

    private Integer resolveDonationNgoId(Donation donation) {
        if (donation.getCampaign() != null
                && donation.getCampaign().getNgo() != null
                && donation.getCampaign().getNgo().getNgoId() != null) {
            return donation.getCampaign().getNgo().getNgoId();
        }

        return donation.getNgo() != null ? donation.getNgo().getNgoId() : null;
    }

    private Donation getReceiptDonationOrThrow(DonationReceipt receipt) {
        Integer donationId = receipt != null
                && receipt.getDonation() != null
                ? receipt.getDonation().getDonationId()
                : null;
        if (donationId == null) {
            throw new IllegalArgumentException("Receipt donation is required");
        }

        return donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException(
                        "Donation not found with id: " + donationId));
    }
}
