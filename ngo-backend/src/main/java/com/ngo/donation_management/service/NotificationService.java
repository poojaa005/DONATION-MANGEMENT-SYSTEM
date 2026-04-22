package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationItem;
import com.ngo.donation_management.entity.DonationRequest;
import com.ngo.donation_management.entity.DonationReceipt;
import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.entity.Payment;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.entity.UrgentNeeds;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.UserNotification;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.DonationItemRepository;
import com.ngo.donation_management.repository.PaymentRepository;
import com.ngo.donation_management.repository.UserRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    private EmailService emailService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserNotificationService userNotificationService;

    public void notifyDonorRegistrationSuccess(User donor) {
        if (donor.getRole() != User.Role.donor) {
            return;
        }

        pushUserUpdate(
                donor,
                "Registration successful",
                "Welcome to the donor portal. Your account has been created successfully.",
                UserNotification.NotificationType.success,
                "/dashboard",
                "registration-success-" + donor.getUserId(),
                () -> emailService.sendRegistrationSuccessEmail(
                        donor.getEmail(), donor.getName())
        );
    }

    public void notifyVolunteerRegistrationSuccess(User volunteerUser) {
        if (volunteerUser.getRole() != User.Role.volunteer) {
            return;
        }

        pushUserUpdate(
                volunteerUser,
                "Registration successful",
                "Welcome to the volunteer portal. Your account is ready and you can start accepting pickup tasks.",
                UserNotification.NotificationType.success,
                "/volunteer",
                "volunteer-registration-success-" + volunteerUser.getUserId(),
                () -> emailService.sendVolunteerRegistrationSuccessEmail(
                        volunteerUser.getEmail(),
                        volunteerUser.getName())
        );
    }

    public void notifyNgoCreated(Ngo ngo, User appAdmin) {
        if (ngo == null) {
            return;
        }

        if (appAdmin != null && appAdmin.getRole() == User.Role.admin) {
            pushUserUpdate(
                    appAdmin,
                    "NGO created",
                    ngo.getNgoName() + " has been added to the system successfully.",
                    UserNotification.NotificationType.success,
                    "/admin/ngos",
                    "app-admin-ngo-created-" + ngo.getNgoId(),
                    null
            );
        }

        if (ngo.getEmail() != null && !ngo.getEmail().isBlank()) {
            safeRunEmail(() -> emailService.sendNgoCreatedEmail(
                    ngo.getEmail(),
                    ngo.getNgoName(),
                    ngo.getCity(),
                    ngo.getState(),
                    null,
                    null
            ));
        }
    }

    public void notifyNgoAdminAssigned(User ngoAdmin,
                                       String rawPassword,
                                       boolean createdByAppAdmin) {
        if (ngoAdmin == null || ngoAdmin.getRole() != User.Role.ngo_admin || ngoAdmin.getNgo() == null) {
            return;
        }

        Ngo ngo = ngoAdmin.getNgo();
        String loginEmail = ngoAdmin.getEmail();
        String eventKey = "ngo-admin-assigned-" + ngoAdmin.getUserId() + "-" + ngo.getNgoId();

        pushUserUpdate(
                ngoAdmin,
                "NGO admin assigned",
                "You are now the NGO admin for " + ngo.getNgoName() + ".",
                UserNotification.NotificationType.success,
                "/admin",
                eventKey,
                () -> emailService.sendNgoAdminAssignedEmail(
                        ngoAdmin.getEmail(),
                        ngoAdmin.getName(),
                        ngo.getNgoName(),
                        loginEmail,
                        rawPassword)
        );

        if (createdByAppAdmin) {
            userRepository.findByRole(User.Role.admin).forEach(appAdmin ->
                    pushUserUpdate(
                            appAdmin,
                            "NGO admin assigned",
                            ngoAdmin.getName() + " is now NGO admin for " + ngo.getNgoName() + ".",
                            UserNotification.NotificationType.success,
                            "/admin/ngos",
                            "app-admin-ngo-admin-assigned-" + ngoAdmin.getUserId(),
                            null
                    ));
        }

        if (ngo.getEmail() != null && !ngo.getEmail().isBlank()) {
            safeRunEmail(() -> emailService.sendNgoCreatedEmail(
                    ngo.getEmail(),
                    ngo.getNgoName(),
                    ngo.getCity(),
                    ngo.getState(),
                    loginEmail,
                    rawPassword
            ));
        }
    }

    public void notifyDonorLoginAlert(User donor,
                                      String ipAddress,
                                      String userAgent) {
        if (donor.getRole() != User.Role.donor) {
            return;
        }

        String loginTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        pushUserUpdate(
                donor,
                "Login alert",
                "A new login was detected for your donor account at " + loginTime + ".",
                UserNotification.NotificationType.info,
                "/dashboard",
                null,
                () -> emailService.sendLoginAlertEmail(
                        donor.getEmail(),
                        donor.getName(),
                        ipAddress,
                        userAgent,
                        loginTime)
        );
    }

    public void notifyDonorDonationSuccess(Payment payment) {
        Donation donation = payment.getDonation();
        User donor = donation.getUser();
        Ngo donationNgo = resolveNgo(donation);
        String ngoName = donationNgo != null
                ? donationNgo.getNgoName()
                : "Direct donation";
        String amount = formatAmount(payment.getAmount());
        String actionUrl = "/donations/receipt/" + donation.getDonationId();

        pushUserUpdate(
                donor,
                "Donation successful",
                "Your donation of " + amount + " was confirmed for " + ngoName + ".",
                UserNotification.NotificationType.success,
                actionUrl,
                "donation-success-" + payment.getPaymentId(),
                () -> emailService.sendDonationSuccessEmail(
                        donor.getEmail(),
                        donor.getName(),
                        amount,
                        ngoName,
                        valueOrDash(payment.getTransactionId()),
                        DATE_TIME_FORMATTER.format(payment.getPaymentDate()))
        );
    }

    public void notifyNgoAdminNewDonation(Donation donation) {
        Ngo ngo = resolveNgo(donation);
        if (donation == null || ngo == null) {
            return;
        }

        List<User> ngoAdmins = getNgoAdmins(ngo.getNgoId());
        if (ngoAdmins.isEmpty()) {
            return;
        }

        String donorName = donation.getUser() != null ? donation.getUser().getName() : "Donor";
        String donationSummary = buildDonationSummary(donation);
        String ngoName = ngo.getNgoName();
        String campaignName = donation.getCampaign() != null
                ? donation.getCampaign().getTitle()
                : "Direct donation";

        ngoAdmins.forEach(ngoAdmin -> pushUserUpdate(
                ngoAdmin,
                "New donation",
                donorName + " made a new donation for " + ngoName + ".",
                UserNotification.NotificationType.info,
                "/admin/reports",
                "ngo-admin-donation-" + donation.getDonationId() + "-user-" + ngoAdmin.getUserId(),
                () -> emailService.sendNgoAdminNewDonationEmail(
                        ngoAdmin.getEmail(),
                        ngoAdmin.getName(),
                        ngoName,
                        donorName,
                        donationSummary,
                        campaignName)
        ));
    }

    public void notifyAppAdminDonationActivity(Donation donation) {
        if (donation == null) {
            return;
        }

        String donorName = donation.getUser() != null ? donation.getUser().getName() : "Donor";
        String summary = buildDonationSummary(donation);
        Ngo ngo = resolveNgo(donation);
        String ngoName = ngo != null ? ngo.getNgoName() : "Direct donation";
        String campaignName = donation.getCampaign() != null
                ? donation.getCampaign().getTitle()
                : "Direct donation";

        userRepository.findByRole(User.Role.admin).forEach(appAdmin ->
                pushUserUpdate(
                        appAdmin,
                        "Donation activity",
                        donorName + " submitted a donation. " + summary + ".",
                        UserNotification.NotificationType.info,
                        "/admin/reports",
                        "app-admin-donation-activity-" + donation.getDonationId() + "-user-" + appAdmin.getUserId(),
                        () -> emailService.sendAppAdminDonationActivityEmail(
                                appAdmin.getEmail(),
                                appAdmin.getName(),
                                donorName,
                                summary,
                                ngoName,
                                campaignName)
                ));
    }

    public void notifyNgoAdminDonationRequestSubmitted(DonationRequest request) {
        Ngo ngo = resolveNgo(request);
        if (request == null || ngo == null) {
            return;
        }

        List<User> ngoAdmins = getNgoAdmins(ngo.getNgoId());
        if (ngoAdmins.isEmpty()) {
            return;
        }

        String donorName = request.getUser() != null ? request.getUser().getName() : "Donor";
        String ngoName = ngo.getNgoName();
        String campaignName = request.getCampaign() != null
                ? request.getCampaign().getTitle()
                : "Campaign";
        String requestSummary = buildDonationRequestSummary(request);
        String donationType = request.getDonationType() != null
                ? request.getDonationType().name()
                : "-";

        ngoAdmins.forEach(ngoAdmin -> pushUserUpdate(
                ngoAdmin,
                "New donation request",
                donorName + " submitted a donation request for " + ngoName + ".",
                UserNotification.NotificationType.info,
                "/admin/requests",
                "ngo-admin-donation-request-" + request.getRequestId() + "-user-" + ngoAdmin.getUserId(),
                () -> emailService.sendDonationRequestAlertEmail(
                        ngoAdmin.getEmail(),
                        ngoAdmin.getName(),
                        donorName,
                        ngoName,
                        campaignName,
                        donationType,
                        requestSummary)
        ));
    }

    public void notifyAppAdminDonationRequestSubmitted(DonationRequest request) {
        if (request == null) {
            return;
        }

        Ngo ngo = resolveNgo(request);
        String donorName = request.getUser() != null ? request.getUser().getName() : "Donor";
        String ngoName = ngo != null ? ngo.getNgoName() : "NGO Donation System";
        String campaignName = request.getCampaign() != null
                ? request.getCampaign().getTitle()
                : "Campaign";
        String requestSummary = buildDonationRequestSummary(request);
        String donationType = request.getDonationType() != null
                ? request.getDonationType().name()
                : "-";

        userRepository.findByRole(User.Role.admin).forEach(appAdmin ->
                pushUserUpdate(
                        appAdmin,
                        "Donation request submitted",
                        donorName + " submitted a donation request for " + campaignName + ".",
                        UserNotification.NotificationType.info,
                        "/admin/requests",
                        "app-admin-donation-request-" + request.getRequestId() + "-user-" + appAdmin.getUserId(),
                        () -> emailService.sendDonationRequestAlertEmail(
                                appAdmin.getEmail(),
                                appAdmin.getName(),
                                donorName,
                                ngoName,
                                campaignName,
                                donationType,
                                requestSummary)
                ));
    }

    public void notifyDonorPaymentFailure(Payment payment,
                                          String failureReason,
                                          String retryUrl) {
        Donation donation = payment.getDonation();
        User donor = donation.getUser();
        Ngo donationNgo = resolveNgo(donation);
        String ngoName = donationNgo != null
                ? donationNgo.getNgoName()
                : "Direct donation";
        String amount = formatAmount(payment.getAmount());

        pushUserUpdate(
                donor,
                "Payment failed",
                "Your donation payment could not be completed. Please retry the transaction.",
                UserNotification.NotificationType.warning,
                retryUrl,
                "payment-failure-" + payment.getPaymentId(),
                () -> emailService.sendPaymentFailureEmail(
                        donor.getEmail(),
                        donor.getName(),
                        amount,
                        ngoName,
                        failureReason,
                        retryUrl)
        );
    }

    public void notifyDonorReceiptReady(DonationReceipt receipt) {
        Donation donation = receipt.getDonation();
        User donor = donation.getUser();
        Ngo donationNgo = resolveNgo(donation);
        String ngoName = donationNgo != null
                ? donationNgo.getNgoName()
                : "Direct donation";
        String amount = formatAmount(donation.getAmount());
        String actionUrl = "/donations/receipt/" + donation.getDonationId();
        Payment payment = donation != null && donation.getDonationId() != null
                ? findSuccessfulPayment(donation.getDonationId())
                : null;

        pushUserUpdate(
                donor,
                "Receipt ready",
                "Your donation receipt " + receipt.getReceiptNumber() + " is now available in your history.",
                UserNotification.NotificationType.info,
                actionUrl,
                "receipt-ready-" + receipt.getReceiptId(),
                () -> emailService.sendReceiptReadyEmail(
                        donor.getEmail(),
                        donor.getName(),
                        receipt.getReceiptNumber(),
                        amount,
                        ngoName,
                        actionUrl,
                        donation,
                        payment,
                        receipt)
        );
    }

    public void notifyDonorCertificateReady(DonationReceipt receipt) {
        Donation donation = receipt.getDonation();
        User donor = donation.getUser();
        Ngo donationNgo = resolveNgo(donation);
        String ngoName = donationNgo != null
                ? donationNgo.getNgoName()
                : "Direct donation";
        String actionUrl = "/donations/receipt/" + donation.getDonationId();

        pushUserUpdate(
                donor,
                "Certificate ready",
                "Your donation certificate has been generated and is ready to view.",
                UserNotification.NotificationType.success,
                actionUrl,
                "certificate-ready-" + receipt.getReceiptId(),
                () -> emailService.sendCertificateReadyEmail(
                        donor.getEmail(),
                        donor.getName(),
                        ngoName,
                        actionUrl,
                        donation,
                        receipt)
        );
    }

    public void notifyDonorDonationRequestApproved(DonationRequest request) {
        if (request == null || request.getUser() == null) {
            return;
        }

        String campaignName = request.getCampaign() != null
                ? request.getCampaign().getTitle()
                : "Campaign";
        String requestSummary = buildDonationRequestSummary(request);

        pushUserUpdate(
                request.getUser(),
                "Donation request approved",
                "Your request for " + campaignName + " has been approved.",
                UserNotification.NotificationType.success,
                "/donations/history",
                "donation-request-approved-" + request.getRequestId(),
                () -> emailService.sendDonationRequestDecisionEmail(
                        request.getUser().getEmail(),
                        request.getUser().getName(),
                        campaignName,
                        "Approved",
                        requestSummary,
                        null)
        );
    }

    public void notifyDonorDonationRequestRejected(DonationRequest request,
                                                   String rejectionReason) {
        if (request == null || request.getUser() == null) {
            return;
        }

        String campaignName = request.getCampaign() != null
                ? request.getCampaign().getTitle()
                : "Campaign";
        String requestSummary = buildDonationRequestSummary(request);
        String reasonText = valueOrDash(rejectionReason);

        pushUserUpdate(
                request.getUser(),
                "Donation request rejected",
                "Your request for " + campaignName + " has been rejected. Reason: " + reasonText + ".",
                UserNotification.NotificationType.warning,
                "/campaigns/" + (request.getCampaign() != null ? request.getCampaign().getCampaignId() : ""),
                "donation-request-rejected-" + request.getRequestId(),
                () -> emailService.sendDonationRequestDecisionEmail(
                        request.getUser().getEmail(),
                        request.getUser().getName(),
                        campaignName,
                        "Rejected",
                        requestSummary,
                        reasonText)
        );
    }

    public void notifyDonorPickupScheduled(PickupRequest pickup) {
        Donation donation = pickup.getDonation();
        User donor = donation.getUser();

        pushUserUpdate(
                donor,
                "Pickup scheduled",
                "Your donation pickup was scheduled for " + formatDate(pickup.getPickupDate()) + ".",
                UserNotification.NotificationType.info,
                "/donations/history",
                "pickup-scheduled-" + pickup.getPickupId(),
                () -> emailService.sendPickupScheduledEmail(
                        donor.getEmail(),
                        donor.getName(),
                        formatDate(pickup.getPickupDate()),
                        valueOrDash(pickup.getTimeSlot()),
                        pickup.getDonorAddress(),
                        donation.getDonationType().name())
        );
    }

    public void notifyNgoAdminPickupRequest(PickupRequest pickup) {
        Ngo ngo = resolveNgo(pickup != null ? pickup.getDonation() : null);
        if (pickup == null
                || pickup.getDonation() == null
                || ngo == null) {
            return;
        }

        List<User> ngoAdmins = getNgoAdmins(ngo.getNgoId());
        if (ngoAdmins.isEmpty()) {
            return;
        }

        String donorName = pickup.getDonation().getUser() != null
                ? pickup.getDonation().getUser().getName()
                : "Donor";
        String itemSummary = buildDonationItemsSummary(pickup.getDonation());

        ngoAdmins.forEach(ngoAdmin -> pushUserUpdate(
                ngoAdmin,
                "Pickup request",
                donorName + " scheduled a pickup for " + formatDate(pickup.getPickupDate()) + ".",
                UserNotification.NotificationType.info,
                "/admin/requests",
                "ngo-admin-pickup-" + pickup.getPickupId() + "-user-" + ngoAdmin.getUserId(),
                () -> emailService.sendNgoAdminPickupRequestEmail(
                        ngoAdmin.getEmail(),
                        ngoAdmin.getName(),
                        donorName,
                        formatDate(pickup.getPickupDate()),
                        valueOrDash(pickup.getTimeSlot()),
                        pickup.getDonorAddress(),
                        itemSummary)
        ));
    }

    public void notifyDonorPickupApproved(PickupRequest pickup) {
        User donor = pickup.getDonation().getUser();

        pushUserUpdate(
                donor,
                "Pickup approved",
                "Your pickup request has been approved.",
                UserNotification.NotificationType.success,
                "/donations/history",
                "pickup-approved-" + pickup.getPickupId(),
                () -> emailService.sendPickupApprovedEmail(
                        donor.getEmail(),
                        donor.getName(),
                        formatDate(pickup.getPickupDate()),
                        valueOrDash(pickup.getTimeSlot()),
                        pickup.getDonorAddress())
        );
    }

    public void notifyDonorPickupRejected(PickupRequest pickup,
                                          String rejectionReason) {
        if (pickup == null || pickup.getDonation() == null || pickup.getDonation().getUser() == null) {
            return;
        }

        User donor = pickup.getDonation().getUser();
        String reasonText = valueOrDash(rejectionReason);

        pushUserUpdate(
                donor,
                "Pickup request rejected",
                "Your pickup request was cancelled by the admin. Reason: " + reasonText + ".",
                UserNotification.NotificationType.warning,
                "/donations/history",
                "pickup-rejected-" + pickup.getPickupId(),
                () -> emailService.sendPickupRejectedEmail(
                        donor.getEmail(),
                        donor.getName(),
                        formatDate(pickup.getPickupDate()),
                        valueOrDash(pickup.getTimeSlot()),
                        pickup.getDonorAddress(),
                        reasonText)
        );
    }

    public void notifyDonorPickupInProgress(TaskAssignment taskAssignment) {
        PickupRequest pickup = taskAssignment.getPickupRequest();
        User donor = pickup.getDonation().getUser();
        String volunteerName = taskAssignment.getVolunteer().getUser().getName();

        pushUserUpdate(
                donor,
                "Pickup in progress",
                volunteerName + " has started your pickup task.",
                UserNotification.NotificationType.info,
                "/donations/history",
                "pickup-in-progress-" + taskAssignment.getTaskId(),
                () -> emailService.sendPickupInProgressEmail(
                        donor.getEmail(),
                        donor.getName(),
                        volunteerName,
                        formatDate(pickup.getPickupDate()),
                        pickup.getDonorAddress())
        );
    }

    public void notifyDonorPickupCompleted(TaskAssignment taskAssignment) {
        PickupRequest pickup = taskAssignment.getPickupRequest();
        User donor = pickup.getDonation().getUser();
        String volunteerName = taskAssignment.getVolunteer().getUser().getName();

        pushUserUpdate(
                donor,
                "Pickup completed",
                "Your donation pickup has been completed successfully.",
                UserNotification.NotificationType.success,
                "/donations/history",
                "pickup-completed-" + taskAssignment.getTaskId(),
                () -> emailService.sendPickupCompletedEmail(
                        donor.getEmail(),
                        donor.getName(),
                        volunteerName,
                        formatDate(pickup.getPickupDate()))
        );
    }

    public void notifyDonorsUrgentNeed(UrgentNeeds urgentNeeds) {
        List<User> donors = userRepository.findByRole(User.Role.donor);
        String ngoName = urgentNeeds.getNgo() != null
                ? urgentNeeds.getNgo().getNgoName()
                : "NGO Donation System";
        String actionUrl = "/campaigns";

        donors.forEach(donor -> pushUserUpdate(
                donor,
                "Urgent need alert",
                urgentNeeds.getTitle() + " - " + urgentNeeds.getMessage(),
                UserNotification.NotificationType.urgent,
                actionUrl,
                "urgent-need-" + urgentNeeds.getUrgentId() + "-user-" + donor.getUserId(),
                () -> emailService.sendUrgentNeedDonorEmail(
                        donor.getEmail(),
                        donor.getName(),
                        urgentNeeds.getTitle(),
                        urgentNeeds.getMessage(),
                        ngoName)
        ));
    }

    public void notifyUrgentNeedCreated(UrgentNeeds urgentNeeds) {
        if (urgentNeeds == null || urgentNeeds.getAdmin() == null) {
            return;
        }

        pushUserUpdate(
                urgentNeeds.getAdmin(),
                "Urgent need created",
                "Banner confirmation: " + urgentNeeds.getTitle() + " is now saved.",
                UserNotification.NotificationType.success,
                "/admin/urgent-needs",
                "urgent-need-created-" + urgentNeeds.getUrgentId(),
                null
        );
    }

    public void notifyDonorPickupReminder(PickupRequest pickup) {
        User donor = pickup.getDonation().getUser();

        pushUserUpdate(
                donor,
                "Pickup reminder",
                "Reminder: your pickup is scheduled for " + formatDate(pickup.getPickupDate()) + ".",
                UserNotification.NotificationType.info,
                "/donations/history",
                "pickup-reminder-" + pickup.getPickupId() + "-" + pickup.getPickupDate(),
                () -> emailService.sendPickupReminderEmail(
                        donor.getEmail(),
                        donor.getName(),
                        formatDate(pickup.getPickupDate()),
                        valueOrDash(pickup.getTimeSlot()),
                        pickup.getDonorAddress())
        );
    }

    public void notifyVolunteersNewDonationItem(DonationItem donationItem,
                                                PickupRequest pickupRequest) {
        List<Volunteer> activeVolunteers = volunteerRepository
                .findByVolunteerStatus(Volunteer.VolunteerStatus.active);

        Donation donation = donationItem.getDonation();
        String donorName = donation.getUser().getName();
        String itemName = donationItem.getItemName();
        String category = donationItem.getCategory() != null ? donationItem.getCategory() : "General";
        int quantity = donationItem.getQuantity();
        String pickupAddress = pickupRequest.getDonorAddress();
        String pickupDate = formatDate(pickupRequest.getPickupDate());

        for (Volunteer volunteer : activeVolunteers) {
            User user = volunteer.getUser();
            String email = user.getEmail();
            String name = user.getName();

            if (email != null && !email.isEmpty()) {
                emailService.sendNewDonationItemEmail(
                        email, name, donorName, itemName,
                        category, quantity, pickupAddress, pickupDate);
            }
        }
    }

    public void notifyVolunteersPickupAvailable(PickupRequest pickupRequest) {
        if (pickupRequest == null || pickupRequest.getDonation() == null) {
            return;
        }

        List<Volunteer> activeVolunteers = volunteerRepository
                .findByVolunteerStatus(Volunteer.VolunteerStatus.active);
        if (activeVolunteers.isEmpty()) {
            return;
        }

        Donation donation = pickupRequest.getDonation();
        String donorName = donation.getUser() != null ? donation.getUser().getName() : "Donor";
        String pickupAddress = valueOrDash(pickupRequest.getDonorAddress());
        String pickupDate = formatDate(pickupRequest.getPickupDate());
        String timeSlot = valueOrDash(pickupRequest.getTimeSlot());
        String itemSummary = buildDonationItemsSummary(donation);
        String campaignName = donation.getCampaign() != null
                ? donation.getCampaign().getTitle()
                : "Direct donation";

        activeVolunteers.forEach(volunteer -> {
            User user = volunteer.getUser();
            if (user == null) {
                return;
            }

            pushUserUpdate(
                    user,
                    "New donation task available",
                    "A pickup task is available from " + donorName + " on "
                            + pickupDate + " at " + pickupAddress + ".",
                    UserNotification.NotificationType.info,
                    "/volunteer",
                    "volunteer-pickup-available-" + pickupRequest.getPickupId() + "-user-" + user.getUserId(),
                    () -> emailService.sendVolunteerPickupAvailableEmail(
                            user.getEmail(),
                            user.getName(),
                            donorName,
                            pickupAddress,
                            pickupDate,
                            timeSlot,
                            itemSummary,
                            campaignName)
            );
        });
    }

    public void notifyVolunteersUrgentNeed(UrgentNeeds urgentNeeds) {
        List<Volunteer> activeVolunteers = volunteerRepository
                .findByVolunteerStatus(Volunteer.VolunteerStatus.active);

        String title = urgentNeeds.getTitle();
        String message = urgentNeeds.getMessage();
        String startTime = urgentNeeds.getStartTime() != null ? urgentNeeds.getStartTime().toString() : "ASAP";
        String endTime = urgentNeeds.getEndTime() != null ? urgentNeeds.getEndTime().toString() : "TBD";

        for (Volunteer volunteer : activeVolunteers) {
            User user = volunteer.getUser();
            String email = user.getEmail();
            String name = user.getName();

            if (email != null && !email.isEmpty()) {
                emailService.sendUrgentNeedEmail(email, name, title, message, startTime, endTime);
            }
        }
    }

    public void notifyVolunteerTaskAssigned(TaskAssignment taskAssignment) {
        Volunteer volunteer = taskAssignment.getVolunteer();
        User user = volunteer.getUser();
        PickupRequest pickup = taskAssignment.getPickupRequest();
        Donation donation = pickup.getDonation();
        User donor = donation.getUser();
        String donorName = donor.getName();
        String donorPhone = valueOrDash(pickup.getDonorPhone());
        String pickupAddress = pickup.getDonorAddress();
        String pickupDate = formatDate(pickup.getPickupDate());
        String timeSlot = valueOrDash(pickup.getTimeSlot());
        String campaignName = donation.getCampaign() != null
                ? donation.getCampaign().getTitle()
                : "Direct donation";
        String itemSummary = buildDonationItemsSummary(donation);

        pushUserUpdate(
                user,
                "Task assigned",
                "Pickup #" + pickup.getPickupId() + " has been assigned. Donor: "
                        + donorName + ". Pickup at " + pickupAddress + " on "
                        + pickupDate + " (" + timeSlot + ").",
                UserNotification.NotificationType.info,
                "/volunteer",
                "volunteer-task-assigned-" + taskAssignment.getTaskId(),
                () -> emailService.sendTaskAssignedEmail(
                        user.getEmail(),
                        user.getName(),
                        taskAssignment.getTaskId(),
                        donorName,
                        donorPhone,
                        pickupAddress,
                        pickupDate,
                        timeSlot,
                        itemSummary,
                        campaignName)
        );
    }

    public void notifyVolunteerTaskReminder(TaskAssignment taskAssignment) {
        Volunteer volunteer = taskAssignment.getVolunteer();
        User user = volunteer.getUser();
        PickupRequest pickup = taskAssignment.getPickupRequest();

        pushUserUpdate(
                user,
                "Task reminder",
                "Reminder: pickup #" + pickup.getPickupId() + " is scheduled for "
                        + formatDate(pickup.getPickupDate()) + " at "
                        + valueOrDash(pickup.getTimeSlot()) + " near "
                        + pickup.getDonorAddress() + ".",
                UserNotification.NotificationType.info,
                "/volunteer",
                "volunteer-task-reminder-" + taskAssignment.getTaskId() + "-" + pickup.getPickupDate(),
                null
        );
    }

    public void notifyVolunteerTaskStarted(TaskAssignment taskAssignment) {
        Volunteer volunteer = taskAssignment.getVolunteer();
        User user = volunteer.getUser();
        PickupRequest pickup = taskAssignment.getPickupRequest();

        pushUserUpdate(
                user,
                "Task started",
                "Pickup #" + pickup.getPickupId() + " is now in progress.",
                UserNotification.NotificationType.info,
                "/volunteer",
                "volunteer-task-started-" + taskAssignment.getTaskId(),
                null
        );
    }

    public void notifyVolunteerTaskCompleted(TaskAssignment taskAssignment) {
        Volunteer volunteer = taskAssignment.getVolunteer();
        User user = volunteer.getUser();
        PickupRequest pickup = taskAssignment.getPickupRequest();
        User donor = pickup.getDonation().getUser();
        String completionTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());

        pushUserUpdate(
                user,
                "Task completed",
                "Pickup #" + pickup.getPickupId() + " has been completed successfully.",
                UserNotification.NotificationType.success,
                "/volunteer",
                "volunteer-task-completed-" + taskAssignment.getTaskId(),
                () -> emailService.sendVolunteerTaskCompletedEmail(
                        user.getEmail(),
                        user.getName(),
                        taskAssignment.getTaskId(),
                        donor.getName(),
                        completionTime)
        );
    }

    public void notifyNgoAdminVolunteerTaskUpdate(TaskAssignment taskAssignment) {
        Ngo ngo = resolveNgo(taskAssignment != null
                && taskAssignment.getPickupRequest() != null
                ? taskAssignment.getPickupRequest().getDonation()
                : null);
        if (taskAssignment == null
                || taskAssignment.getPickupRequest() == null
                || taskAssignment.getPickupRequest().getDonation() == null
                || ngo == null) {
            return;
        }

        List<User> ngoAdmins = getNgoAdmins(ngo.getNgoId());
        if (ngoAdmins.isEmpty()) {
            return;
        }

        String volunteerName = taskAssignment.getVolunteer().getUser().getName();
        String status = taskAssignment.getTaskStatus().name().replace('_', ' ');

        ngoAdmins.forEach(ngoAdmin -> pushUserUpdate(
                ngoAdmin,
                "Volunteer task update",
                volunteerName + " changed pickup #" + taskAssignment.getPickupRequest().getPickupId()
                        + " to " + status + ".",
                UserNotification.NotificationType.info,
                "/admin/requests",
                "ngo-admin-task-update-" + taskAssignment.getTaskId() + "-" + taskAssignment.getTaskStatus()
                        + "-user-" + ngoAdmin.getUserId(),
                null
        ));
    }

    public void notifyDonorPickupConfirmed(TaskAssignment taskAssignment) {
        PickupRequest pickup = taskAssignment.getPickupRequest();
        Donation donation = pickup.getDonation();
        User donor = donation.getUser();
        User volunteerUser = taskAssignment.getVolunteer().getUser();

        String volunteerName = volunteerUser.getName();
        String pickupDate = formatDate(pickup.getPickupDate());
        String timeSlot = pickup.getTimeSlot() != null ? pickup.getTimeSlot() : "Flexible";

        pushUserUpdate(
                donor,
                "Volunteer assigned",
                volunteerName + " selected your pickup scheduled for " + pickupDate + ".",
                UserNotification.NotificationType.success,
                "/donations/history",
                "pickup-confirmed-" + pickup.getPickupId(),
                () -> emailService.sendPickupConfirmedEmail(
                        donor.getEmail(),
                        donor.getName(),
                        volunteerName,
                        pickupDate,
                        timeSlot)
        );
    }

    public void notifyNgoAdminVolunteerTaskAccepted(TaskAssignment taskAssignment) {
        notifyVolunteerTaskAcceptedToAdmins(taskAssignment, true);
    }

    public void notifyAppAdminVolunteerTaskAccepted(TaskAssignment taskAssignment) {
        notifyVolunteerTaskAcceptedToAdmins(taskAssignment, false);
    }

    public void notifyAppAdminsSystemError(String context,
                                           String details) {
        userRepository.findByRole(User.Role.admin).forEach(appAdmin -> {
            if (appAdmin.getEmail() == null || appAdmin.getEmail().isBlank()) {
                return;
            }

            safeRunEmail(() -> emailService.sendSystemErrorEmail(
                    appAdmin.getEmail(),
                    appAdmin.getName(),
                    context,
                    details
            ));
        });
    }

    private void pushUserUpdate(User user,
                                String title,
                                String message,
                                UserNotification.NotificationType type,
                                String actionUrl,
                                String eventKey,
                                Runnable emailAction) {
        if (user == null) {
            return;
        }

        if (eventKey != null && userNotificationService.exists(user, eventKey)) {
            return;
        }

        userNotificationService.createNotification(
                user, title, message, type, actionUrl, eventKey);

        if (emailAction != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            safeRunEmail(emailAction);
        }
    }

    private void safeRunEmail(Runnable emailAction) {
        try {
            emailAction.run();
        } catch (RuntimeException ignored) {
            // Email delivery must not break the main business action.
        }
    }

    private List<User> getNgoAdmins(Integer ngoId) {
        return userRepository.findByRole(User.Role.ngo_admin).stream()
                .filter(user -> user.getNgo() != null
                        && user.getNgo().getNgoId() != null
                        && user.getNgo().getNgoId().equals(ngoId))
                .toList();
    }

    private String buildDonationSummary(Donation donation) {
        if (donation.getDonationType() == Donation.DonationType.monetary) {
            return formatAmount(donation.getAmount());
        }

        return buildDonationItemsSummary(donation);
    }

    private String buildDonationRequestSummary(DonationRequest request) {
        if (request == null) {
            return "-";
        }

        if (request.getDonationType() == DonationRequest.DonationType.monetary) {
            return formatAmount(request.getAmount());
        }

        return valueOrDash(request.getRequestMessage());
    }

    private String buildDonationItemsSummary(Donation donation) {
        if (donation == null || donation.getDonationId() == null) {
            return "Items will be shared by the admin";
        }

        List<DonationItem> items = donationItemRepository.findByDonation_DonationId(donation.getDonationId());
        if (items.isEmpty()) {
            return "Items will be shared by the admin";
        }

        return items.stream()
                .map(item -> item.getItemName() + " x" + item.getQuantity())
                .limit(5)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Items will be shared by the admin");
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "Rupees 0.00";
        }

        return "Rupees " + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private Payment findSuccessfulPayment(Integer donationId) {
        if (donationId == null) {
            return null;
        }

        return paymentRepository.findByDonation_DonationId(donationId).stream()
                .filter(payment -> payment.getPaymentStatus() == Payment.PaymentStatus.success)
                .findFirst()
                .orElse(null);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Ngo resolveNgo(Donation donation) {
        if (donation == null) {
            return null;
        }

        if (donation.getCampaign() != null && donation.getCampaign().getNgo() != null) {
            return donation.getCampaign().getNgo();
        }

        return donation.getNgo();
    }

    private Ngo resolveNgo(DonationRequest request) {
        if (request == null || request.getCampaign() == null) {
            return null;
        }

        return request.getCampaign().getNgo();
    }

    private void notifyVolunteerTaskAcceptedToAdmins(TaskAssignment taskAssignment,
                                                     boolean ngoAdminsOnly) {
        if (taskAssignment == null
                || taskAssignment.getPickupRequest() == null
                || taskAssignment.getPickupRequest().getDonation() == null
                || taskAssignment.getVolunteer() == null
                || taskAssignment.getVolunteer().getUser() == null) {
            return;
        }

        PickupRequest pickup = taskAssignment.getPickupRequest();
        Donation donation = pickup.getDonation();
        Ngo ngo = resolveNgo(donation);
        String donorName = donation.getUser() != null ? donation.getUser().getName() : "Donor";
        String volunteerName = taskAssignment.getVolunteer().getUser().getName();
        String pickupDate = formatDate(pickup.getPickupDate());
        String timeSlot = valueOrDash(pickup.getTimeSlot());
        String pickupAddress = valueOrDash(pickup.getDonorAddress());
        String ngoName = ngo != null ? ngo.getNgoName() : "Direct donation";

        List<User> recipients = ngoAdminsOnly
                ? (ngo != null ? getNgoAdmins(ngo.getNgoId()) : List.of())
                : userRepository.findByRole(User.Role.admin);

        recipients.forEach(adminUser -> pushUserUpdate(
                adminUser,
                "Volunteer selected pickup",
                volunteerName + " selected pickup #" + pickup.getPickupId() + ".",
                UserNotification.NotificationType.info,
                "/admin/requests",
                (ngoAdminsOnly ? "ngo-admin-task-accepted-" : "app-admin-task-accepted-")
                        + taskAssignment.getTaskId() + "-user-" + adminUser.getUserId(),
                () -> emailService.sendVolunteerTaskAcceptedAdminEmail(
                        adminUser.getEmail(),
                        adminUser.getName(),
                        volunteerName,
                        donorName,
                        pickupAddress,
                        pickupDate,
                        timeSlot,
                        ngoName)
        ));
    }
}
