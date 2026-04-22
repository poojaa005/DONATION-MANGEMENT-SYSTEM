package com.ngo.donation_management.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromPhoneNumber;

    private boolean configured;

    @PostConstruct
    public void init() {
        configured = accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromPhoneNumber != null && !fromPhoneNumber.isBlank();

        if (configured) {
            Twilio.init(accountSid, authToken);
        }
    }

    @Async
    public void sendSms(String toPhone, String messageBody) {
        try {
            if (!configured || toPhone == null || toPhone.isBlank()) {
                return;
            }

            // Ensure phone number has country code (default +91 for India)
            String formattedPhone = toPhone.startsWith("+") ? toPhone : "+91" + toPhone;
            Message.creator(
                    new PhoneNumber(formattedPhone),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Failed to send SMS to " + toPhone + ": " + e.getMessage());
        }
    }

    // ---- Volunteer SMS: New Donation Item ----
    @Async
    public void sendNewDonationItemSms(String volunteerPhone, String volunteerName,
                                        String itemName, String pickupAddress,
                                        String pickupDate) {
        String msg = String.format(
            "[NGO] Hi %s, a new donation item \"%s\" is available for pickup at %s on %s. Please check the volunteer portal.",
            volunteerName, itemName, pickupAddress, pickupDate
        );
        sendSms(volunteerPhone, msg);
    }

    // ---- Volunteer SMS: Urgent Help Needed ----
    @Async
    public void sendUrgentNeedSms(String volunteerPhone, String volunteerName,
                                   String urgentTitle) {
        String msg = String.format(
            "[NGO] URGENT Hi %s: \"%s\" - Immediate help is needed. Please log in to the volunteer portal now.",
            volunteerName, urgentTitle
        );
        sendSms(volunteerPhone, msg);
    }

    // ---- Volunteer SMS: Task Assigned ----
    @Async
    public void sendTaskAssignedSms(String volunteerPhone, String volunteerName,
                                     int taskId, String pickupAddress, String pickupDate) {
        String msg = String.format(
            "[NGO] Hi %s, Task #%d assigned. Pickup at: %s on %s. Open the app to navigate to donor location.",
            volunteerName, taskId, pickupAddress, pickupDate
        );
        sendSms(volunteerPhone, msg);
    }

    // ---- Donor SMS: Pickup Confirmed ----
    @Async
    public void sendPickupConfirmedSms(String donorPhone, String donorName,
                                        String volunteerName, String pickupDate,
                                        String timeSlot) {
        String msg = String.format(
            "[NGO] Hi %s, volunteer %s will pick up your donation on %s (%s). Thank you!",
            donorName, volunteerName, pickupDate, timeSlot
        );
        sendSms(donorPhone, msg);
    }
}
