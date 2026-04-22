package com.ngo.donation_management.service;

// service/PaymentService.java
import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationReceipt;
import com.ngo.donation_management.entity.Payment;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.CampaignRepository;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DonationReceiptService donationReceiptService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AccessScopeService accessScopeService;

    @Transactional
    public Payment createPayment(Payment payment) {
        Donation donation = getDonationOrThrow(payment);
        ensureDonationAccess(donation);
        boolean alreadySuccessful =
                donation.getDonationStatus() == Donation.DonationStatus.completed;
        payment.setDonation(donation);

        Payment savedPayment = paymentRepository.save(payment);
        handleSuccessfulPayment(savedPayment, donation, alreadySuccessful);
        handleFailedPayment(savedPayment, donation);
        return savedPayment;
    }

    public List<Payment> getAllPayments() {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return paymentRepository.findByScopedNgoId(scopedNgoId.get());
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return paymentRepository.findAll();
        }

        return filterPaymentsForCurrentScope(paymentRepository.findAll());
    }

    private List<Payment> filterPaymentsForCurrentScope(List<Payment> payments) {
        return payments.stream()
                .filter(this::canAccessPayment)
                .toList();
    }

    public Optional<Payment> getPaymentById(Integer id) {
        return paymentRepository.findById(id)
                .filter(this::canAccessPayment);
    }

    public List<Payment> getByDonationId(Integer donationId) {
        return paymentRepository.findByDonation_DonationId(donationId).stream()
                .filter(this::canAccessPayment)
                .toList();
    }

    public List<Payment> getByStatus(String status) {
        Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(
                status.toLowerCase());
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return paymentRepository.findByPaymentStatusAndScopedNgoId(
                    paymentStatus, scopedNgoId.get());
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return paymentRepository.findByPaymentStatus(paymentStatus);
        }

        return filterPaymentsForCurrentScope(
                paymentRepository.findByPaymentStatus(paymentStatus));
    }

    public List<Payment> getByMethod(String method) {
        Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(
                method.toLowerCase());
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return paymentRepository.findByPaymentMethodAndScopedNgoId(
                    paymentMethod, scopedNgoId.get());
        }

        if (accessScopeService.isAppAdmin(currentUser)) {
            return paymentRepository.findByPaymentMethod(paymentMethod);
        }

        return filterPaymentsForCurrentScope(
                paymentRepository.findByPaymentMethod(paymentMethod));
    }

    public Optional<Payment> getByTransactionId(
            String transactionId) {
        return paymentRepository
                .findByTransactionId(transactionId)
                .filter(this::canAccessPayment);
    }

    @Transactional
    public Payment updatePayment(Integer id,
                                 Payment updatedPayment) {
        return paymentRepository.findById(id).map(payment -> {
            ensurePaymentAccess(payment);
            Donation donation = getDonationOrThrow(updatedPayment);
            ensureDonationAccess(donation);
            boolean alreadySuccessful =
                    payment.getPaymentStatus() == Payment.PaymentStatus.success
                            || donation.getDonationStatus() == Donation.DonationStatus.completed;
            payment.setDonation(donation);
            payment.setPaymentMethod(
                    updatedPayment.getPaymentMethod());
            payment.setTransactionId(
                    updatedPayment.getTransactionId());
            payment.setAmount(updatedPayment.getAmount());
            payment.setPaymentStatus(
                    updatedPayment.getPaymentStatus());
            Payment savedPayment = paymentRepository.save(payment);
            handleSuccessfulPayment(savedPayment, donation, alreadySuccessful);
            handleFailedPayment(savedPayment, donation);
            return savedPayment;
        }).orElseThrow(() -> new RuntimeException(
                "Payment not found with id: " + id));
    }

    public void deletePayment(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found with id: " + id));
        ensurePaymentAccess(payment);
        paymentRepository.delete(payment);
    }

    public BigDecimal getTotalSuccessfulAmount() {
        User currentUser = accessScopeService.requireCurrentUser();
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return paymentRepository.getTotalSuccessfulPaymentAmountByScopedNgoId(
                    scopedNgoId.get());
        }

        if (!accessScopeService.isAppAdmin(currentUser)) {
            return getByStatus(Payment.PaymentStatus.success.name()).stream()
                    .map(Payment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return paymentRepository.getTotalSuccessfulPaymentAmount();
    }

    private Donation getDonationOrThrow(Payment payment) {
        Integer donationId = payment.getDonation() != null
                ? payment.getDonation().getDonationId()
                : null;

        if (donationId == null) {
            throw new RuntimeException("Donation is required for payment");
        }

        return donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException(
                        "Donation not found with id: " + donationId));
    }

    private void handleSuccessfulPayment(Payment payment,
                                         Donation donation,
                                         boolean alreadySuccessful) {
        if (payment.getPaymentStatus() != Payment.PaymentStatus.success) {
            return;
        }

        if (!alreadySuccessful) {
            donation.setDonationStatus(Donation.DonationStatus.completed);
            donationRepository.save(donation);
            updateCampaignCollectedAmount(donation, payment.getAmount());
        }

        DonationReceipt receipt = donationReceiptService.createOrGetReceipt(donation, payment);
        notificationService.notifyDonorDonationSuccess(payment);
        notificationService.notifyDonorReceiptReady(receipt);
        notificationService.notifyDonorCertificateReady(receipt);
        notificationService.notifyNgoAdminNewDonation(donation);
        notificationService.notifyAppAdminDonationActivity(donation);
    }

    private void updateCampaignCollectedAmount(Donation donation,
                                               BigDecimal paymentAmount) {
        Campaign campaign = donation.getCampaign();
        if (campaign == null || paymentAmount == null) {
            return;
        }

        BigDecimal existingAmount = campaign.getCollectedAmount() != null
                ? campaign.getCollectedAmount()
                : BigDecimal.ZERO;
        campaign.setCollectedAmount(existingAmount.add(paymentAmount));
        campaignRepository.save(campaign);
    }

    private void handleFailedPayment(Payment payment,
                                     Donation donation) {
        if (payment.getPaymentStatus() != Payment.PaymentStatus.failed) {
            return;
        }

        String retryUrl = donation.getCampaign() != null
                ? "/donate/" + donation.getCampaign().getCampaignId()
                : "/campaigns";
        notificationService.notifyDonorPaymentFailure(
                payment,
                "The payment could not be processed.",
                retryUrl
        );
    }

    private boolean canAccessPayment(Payment payment) {
        try {
            ensurePaymentAccess(payment);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void ensurePaymentAccess(Payment payment) {
        if (payment != null && payment.getDonation() != null) {
            ensureDonationAccess(payment.getDonation());
        } else if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new IllegalArgumentException(
                    "NGO admins can only view payments linked to their NGO campaigns");
        }
    }

    private void ensureDonationAccess(Donation donation) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        Integer donorUserId = donation != null
                && donation.getUser() != null
                ? donation.getUser().getUserId()
                : null;
        if (accessScopeService.isDonor(currentUser)) {
            if (donorUserId == null
                    || !currentUser.getUserId().equals(donorUserId)) {
                throw new AccessDeniedException(
                        "You can only access your own payments");
            }
            return;
        }

        Integer donationNgoId = resolveDonationNgoId(donation);
        if (donationNgoId != null) {
            accessScopeService.ensureNgoAccess(donationNgoId);
        } else if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new IllegalArgumentException(
                    "NGO admins can only view payments linked to their NGO");
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    private Integer resolveDonationNgoId(Donation donation) {
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
