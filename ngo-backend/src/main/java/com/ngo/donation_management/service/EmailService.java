package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationReceipt;
import com.ngo.donation_management.entity.Payment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ReceiptDocumentService receiptDocumentService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        sendEmailWithAttachments(toEmail, subject, htmlContent, List.of());
    }

    @Async
    public void sendEmailWithAttachments(String toEmail,
                                         String subject,
                                         String htmlContent,
                                         List<EmailAttachment> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            for (EmailAttachment attachment : attachments) {
                helper.addAttachment(
                        attachment.fileName(),
                        new ByteArrayResource(attachment.content()),
                        attachment.contentType()
                );
            }

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        }
    }

    public void sendRegistrationOtpEmail(String recipientEmail, String recipientName,
                                         String otpCode, int expiryMinutes) {
        String subject = "Verify your email with OTP";
        String html = buildEmailLayout(
                "Email Verification",
                recipientName,
                "Use the OTP below to complete your NGO Donation System registration.",
                """
                        <div style="margin:24px 0;padding:20px;text-align:center;background:#f5f7fb;border-radius:8px;border:1px solid #d7deed;">
                          <span style="font-size:30px;letter-spacing:8px;font-weight:bold;color:#1565c0;">%s</span>
                        </div>
                        <p>This OTP is valid for %d minutes.</p>
                        <p>If you did not request this registration, you can ignore this email.</p>
                        """.formatted(otpCode, expiryMinutes),
                "#1565c0"
        );
        sendEmail(recipientEmail, subject, html);
    }

    public void sendRegistrationSuccessEmail(String recipientEmail, String recipientName) {
        sendEmail(recipientEmail,
                "Welcome to NGO Donation System",
                buildEmailLayout(
                        "Welcome",
                        recipientName,
                        "Your donor account has been created successfully.",
                        "<p>You can now log in, donate to campaigns, request pickups, and track your donation history.</p>",
                        "#2e7d32"
                ));
    }

    public void sendVolunteerRegistrationSuccessEmail(String recipientEmail, String recipientName) {
        sendEmail(recipientEmail,
                "Welcome to the Volunteer Team",
                buildEmailLayout(
                        "Volunteer Account Ready",
                        recipientName,
                        "Your volunteer account has been created successfully.",
                        "<p>You can now open the volunteer dashboard, review available pickups, and manage your assigned tasks.</p>",
                        "#0f766e"
                ));
    }

    public void sendNgoCreatedEmail(String recipientEmail,
                                    String ngoName,
                                    String ngoCity,
                                    String ngoState,
                                    String loginEmail,
                                    String loginPassword) {
        String credentialsText = (loginEmail == null || loginEmail.isBlank())
                ? "<p>Login credentials will be shared once an NGO admin account is assigned.</p>"
                : buildDetailsTable(new String[][]{
                {"Login Email", loginEmail},
                {"Temporary Password", valueOrDash(loginPassword)}
        });

        sendEmail(recipientEmail,
                "NGO Account Created",
                buildEmailLayout(
                        "NGO Account Created",
                        ngoName,
                        "Your NGO profile has been created in the NGO Donation Management System.",
                        buildDetailsTable(new String[][]{
                                {"NGO", ngoName},
                                {"Location", valueOrDash(ngoCity) + ", " + valueOrDash(ngoState)},
                                {"Status", "Active in system"}
                        }) + credentialsText,
                        "#1565c0"
                ));
    }

    public void sendNgoAdminAssignedEmail(String recipientEmail,
                                          String recipientName,
                                          String ngoName,
                                          String loginEmail,
                                          String loginPassword) {
        sendEmail(recipientEmail,
                "You are now NGO Admin",
                buildEmailLayout(
                        "NGO Admin Assigned",
                        recipientName,
                        "You have been assigned as the NGO admin for " + ngoName + ".",
                        buildDetailsTable(new String[][]{
                                {"NGO", ngoName},
                                {"Login Email", loginEmail},
                                {"Temporary Password", valueOrDash(loginPassword)},
                                {"Role", "NGO Admin"}
                        }),
                        "#0f766e"
                ));
    }

    public void sendLoginAlertEmail(String recipientEmail, String recipientName,
                                    String ipAddress, String userAgent, String loginTime) {
        sendEmail(recipientEmail,
                "Login Alert",
                buildEmailLayout(
                        "New Login Detected",
                        recipientName,
                        "A new login was recorded for your donor account.",
                        buildDetailsTable(new String[][]{
                                {"Time", loginTime},
                                {"IP Address", valueOrDash(ipAddress)},
                                {"Device", valueOrDash(userAgent)}
                        }),
                        "#6a1b9a"
                ));
    }

    public void sendDonationSuccessEmail(String recipientEmail, String recipientName,
                                         String amount, String ngoName,
                                         String transactionId, String donationDate) {
        sendEmail(recipientEmail,
                "Donation Successful",
                buildEmailLayout(
                        "Donation Confirmed",
                        recipientName,
                        "Your payment has been completed successfully.",
                        buildDetailsTable(new String[][]{
                                {"Amount", amount},
                                {"NGO", ngoName},
                                {"Transaction ID", transactionId},
                                {"Date", donationDate}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendPaymentFailureEmail(String recipientEmail, String recipientName,
                                        String amount, String ngoName,
                                        String failureReason, String retryUrl) {
        String extra = buildDetailsTable(new String[][]{
                {"Amount", amount},
                {"NGO", ngoName},
                {"Reason", failureReason}
        }) + buildActionLink("Retry Donation", retryUrl);

        sendEmail(recipientEmail,
                "Payment Failed",
                buildEmailLayout(
                        "Payment Failed",
                        recipientName,
                        "We could not complete your donation payment.",
                        extra,
                        "#c62828"
                ));
    }

    public void sendReceiptReadyEmail(String recipientEmail, String recipientName,
                                      String receiptNumber, String amount,
                                      String ngoName, String receiptUrl,
                                      Donation donation, Payment payment,
                                      DonationReceipt receipt) {
        String extra = buildDetailsTable(new String[][]{
                {"Receipt Number", receiptNumber},
                {"Amount", amount},
                {"NGO", ngoName},
                {"Receipt Status", "Issued"}
        }) + """
                <div style="margin-top:16px;padding:14px;border:1px solid #e5e7eb;border-radius:10px;background:#f8fafc;">
                  This email serves as your donation receipt confirmation. Please keep the receipt number for future reference.
                </div>
                """;

        sendEmailWithAttachments(recipientEmail,
                "Receipt Ready",
                buildEmailLayout(
                        "Donation Receipt",
                        recipientName,
                        "Your donation receipt has been generated successfully.",
                        extra,
                        "#ef6c00"
                ),
                List.of(new EmailAttachment(
                        safeFileName(receiptNumber, "receipt") + ".pdf",
                        receiptDocumentService.generateReceiptPdf(donation, payment, receipt),
                        "application/pdf"
                )));
    }

    public void sendCertificateReadyEmail(String recipientEmail, String recipientName,
                                          String ngoName, String certificateUrl,
                                          Donation donation, DonationReceipt receipt) {
        String extra = buildDetailsTable(new String[][]{
                {"NGO", ngoName},
                {"Status", "Certificate generated"},
                {"Certificate Type", "Donation Certificate"}
        }) + """
                <div style="margin-top:16px;padding:14px;border:1px solid #e5e7eb;border-radius:10px;background:#f8fafc;">
                  This email confirms that your donation certificate has been generated. Please keep this message for your records.
                </div>
                """;

        sendEmailWithAttachments(recipientEmail,
                "Donation Certificate Ready",
                buildEmailLayout(
                        "Certificate Ready",
                        recipientName,
                        "Your donation certificate is ready to view.",
                        extra,
                        "#00897b"
                ),
                List.of(new EmailAttachment(
                        safeFileName(receipt != null ? receipt.getReceiptNumber() : null, "certificate") + ".pdf",
                        receiptDocumentService.generateCertificatePdf(donation, receipt),
                        "application/pdf"
                )));
    }

    public void sendPickupScheduledEmail(String recipientEmail, String recipientName,
                                         String pickupDate, String timeSlot,
                                         String pickupAddress, String donationType) {
        sendEmail(recipientEmail,
                "Pickup Scheduled",
                buildEmailLayout(
                        "Pickup Scheduled",
                        recipientName,
                        "Your donation pickup request has been created successfully.",
                        buildDetailsTable(new String[][]{
                                {"Donation Type", donationType},
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Address", pickupAddress}
                        }),
                        "#1565c0"
                ));
    }

    public void sendPickupApprovedEmail(String recipientEmail, String recipientName,
                                        String pickupDate, String timeSlot,
                                        String pickupAddress) {
        sendEmail(recipientEmail,
                "Pickup Approved",
                buildEmailLayout(
                        "Pickup Approved",
                        recipientName,
                        "Your pickup request has been approved.",
                        buildDetailsTable(new String[][]{
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Address", pickupAddress}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendPickupRejectedEmail(String recipientEmail, String recipientName,
                                        String pickupDate, String timeSlot,
                                        String pickupAddress, String rejectionReason) {
        sendEmail(recipientEmail,
                "Pickup Rejected",
                buildEmailLayout(
                        "Pickup Rejected",
                        recipientName,
                        "Your pickup request could not be approved.",
                        buildDetailsTable(new String[][]{
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Address", pickupAddress},
                                {"Reason", rejectionReason}
                        }),
                        "#c62828"
                ));
    }

    public void sendPickupInProgressEmail(String recipientEmail, String recipientName,
                                          String volunteerName, String pickupDate,
                                          String pickupAddress) {
        sendEmail(recipientEmail,
                "Pickup In Progress",
                buildEmailLayout(
                        "Volunteer Started Pickup",
                        recipientName,
                        "Your assigned volunteer has started the pickup task.",
                        buildDetailsTable(new String[][]{
                                {"Volunteer", volunteerName},
                                {"Pickup Date", pickupDate},
                                {"Address", pickupAddress}
                        }),
                        "#ef6c00"
                ));
    }

    public void sendPickupCompletedEmail(String recipientEmail, String recipientName,
                                         String volunteerName, String pickupDate) {
        sendEmail(recipientEmail,
                "Pickup Completed",
                buildEmailLayout(
                        "Pickup Completed",
                        recipientName,
                        "Your donation pickup has been completed successfully. Thank you for contributing.",
                        buildDetailsTable(new String[][]{
                                {"Volunteer", volunteerName},
                                {"Completed On", pickupDate}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendUrgentNeedDonorEmail(String recipientEmail, String recipientName,
                                         String urgentTitle, String urgentMessage,
                                         String ngoName) {
        sendEmail(recipientEmail,
                "Urgent Need Alert",
                buildEmailLayout(
                        "Urgent Need Alert",
                        recipientName,
                        "A new urgent need has been posted for donors.",
                        buildDetailsTable(new String[][]{
                                {"Campaign", urgentTitle},
                                {"NGO", ngoName},
                                {"Details", urgentMessage}
                        }),
                        "#c62828"
                ));
    }

    public void sendNgoAdminNewDonationEmail(String recipientEmail,
                                             String recipientName,
                                             String ngoName,
                                             String donorName,
                                             String donationSummary,
                                             String campaignName) {
        sendEmail(recipientEmail,
                "New Donation Received",
                buildEmailLayout(
                        "New Donation",
                        recipientName,
                        "A new donation was received for your NGO.",
                        buildDetailsTable(new String[][]{
                                {"NGO", ngoName},
                                {"Donor", donorName},
                                {"Campaign", campaignName},
                                {"Donation", donationSummary}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendAppAdminDonationActivityEmail(String recipientEmail,
                                                  String recipientName,
                                                  String donorName,
                                                  String donationSummary,
                                                  String ngoName,
                                                  String campaignName) {
        sendEmail(recipientEmail,
                "Donation Activity Alert",
                buildEmailLayout(
                        "Donation Activity",
                        recipientName,
                        "A donor submitted a new donation in the platform.",
                        buildDetailsTable(new String[][]{
                                {"Donor", donorName},
                                {"NGO", ngoName},
                                {"Campaign", campaignName},
                                {"Donation", donationSummary}
                        }),
                        "#1565c0"
                ));
    }

    public void sendDonationRequestAlertEmail(String recipientEmail,
                                              String recipientName,
                                              String donorName,
                                              String ngoName,
                                              String campaignName,
                                              String donationType,
                                              String requestSummary) {
        sendEmail(recipientEmail,
                "New Donation Request Received",
                buildEmailLayout(
                        "Donation Request",
                        recipientName,
                        "A donor submitted a new donation request that needs review.",
                        buildDetailsTable(new String[][]{
                                {"Donor", donorName},
                                {"NGO", ngoName},
                                {"Campaign", campaignName},
                                {"Type", donationType},
                                {"Request", requestSummary}
                        }),
                        "#ef6c00"
                ));
    }

    public void sendNgoAdminPickupRequestEmail(String recipientEmail,
                                               String recipientName,
                                               String donorName,
                                               String pickupDate,
                                               String timeSlot,
                                               String pickupAddress,
                                               String itemSummary) {
        sendEmail(recipientEmail,
                "New Pickup Request",
                buildEmailLayout(
                        "Pickup Request",
                        recipientName,
                        "A donor scheduled a pickup linked to your NGO.",
                        buildDetailsTable(new String[][]{
                                {"Donor", donorName},
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Address", pickupAddress},
                                {"Items", itemSummary}
                        }),
                        "#ef6c00"
                ));
    }

    public void sendPickupReminderEmail(String recipientEmail, String recipientName,
                                        String pickupDate, String timeSlot,
                                        String pickupAddress) {
        sendEmail(recipientEmail,
                "Pickup Reminder",
                buildEmailLayout(
                        "Pickup Reminder",
                        recipientName,
                        "This is a reminder for your upcoming donation pickup.",
                        buildDetailsTable(new String[][]{
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Address", pickupAddress}
                        }),
                        "#5d4037"
                ));
    }

    public void sendNewDonationItemEmail(String volunteerEmail, String volunteerName,
                                         String donorName, String itemName,
                                         String category, int quantity,
                                         String pickupAddress, String pickupDate) {
        sendEmail(volunteerEmail,
                "New Donation Item Available",
                buildEmailLayout(
                        "New Donation Available",
                        volunteerName,
                        "A new donation item has been placed and needs pickup.",
                        buildDetailsTable(new String[][]{
                                {"Donor", donorName},
                                {"Item", itemName},
                                {"Category", category},
                                {"Quantity", String.valueOf(quantity)},
                                {"Pickup Address", pickupAddress},
                                {"Pickup Date", pickupDate}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendUrgentNeedEmail(String volunteerEmail, String volunteerName,
                                    String urgentTitle, String urgentMessage,
                                    String startTime, String endTime) {
        sendEmail(volunteerEmail,
                "Urgent Help Needed - " + urgentTitle,
                buildEmailLayout(
                        "Urgent Help Needed",
                        volunteerName,
                        urgentMessage,
                        buildDetailsTable(new String[][]{
                                {"Title", urgentTitle},
                                {"Available From", startTime},
                                {"Available Until", endTime}
                        }),
                        "#c62828"
                ));
    }

    public void sendTaskAssignedEmail(String volunteerEmail, String volunteerName,
                                      int taskId, String donorName, String donorPhone,
                                      String pickupAddress, String pickupDate,
                                      String timeSlot, String itemSummary,
                                      String campaignName) {
        sendEmail(volunteerEmail,
                "Task Assigned - Pickup #" + taskId,
                buildEmailLayout(
                        "Task Assigned",
                        volunteerName,
                        "You have been assigned a new pickup task.",
                        buildDetailsTable(new String[][]{
                                {"Task ID", "#" + taskId},
                                {"Donor", donorName},
                                {"Donor Phone", donorPhone},
                                {"Pickup Address", pickupAddress},
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Items", itemSummary},
                                {"Campaign", campaignName}
                        }),
                        "#1565c0"
                ));
    }

    public void sendVolunteerPickupAvailableEmail(String volunteerEmail,
                                                  String volunteerName,
                                                  String donorName,
                                                  String pickupAddress,
                                                  String pickupDate,
                                                  String timeSlot,
                                                  String itemSummary,
                                                  String campaignName) {
        sendEmail(volunteerEmail,
                "New Donation Task Available",
                buildEmailLayout(
                        "Pickup Task Available",
                        volunteerName,
                        "A newly approved donation pickup is now available for volunteers.",
                        buildDetailsTable(new String[][]{
                                {"Donor", donorName},
                                {"Campaign", campaignName},
                                {"Pickup Address", pickupAddress},
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot},
                                {"Items", itemSummary}
                        }),
                        "#0f766e"
                ));
    }

    public void sendVolunteerTaskCompletedEmail(String volunteerEmail,
                                                String volunteerName,
                                                int taskId,
                                                String donorName,
                                                String completedAt) {
        sendEmail(volunteerEmail,
                "Task Completed - Pickup #" + taskId,
                buildEmailLayout(
                        "Task Completed",
                        volunteerName,
                        "Your pickup task has been marked completed.",
                        buildDetailsTable(new String[][]{
                                {"Task ID", "#" + taskId},
                                {"Donor", donorName},
                                {"Completed At", completedAt},
                                {"Status", "Completed"}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendSystemErrorEmail(String recipientEmail,
                                     String recipientName,
                                     String context,
                                     String details) {
        sendEmail(recipientEmail,
                "System Error Alert",
                buildEmailLayout(
                        "System Error",
                        recipientName,
                        "A system failure was detected and may need investigation.",
                        buildDetailsTable(new String[][]{
                                {"Context", context},
                                {"Details", details}
                        }),
                        "#c62828"
                ));
    }

    public void sendPickupConfirmedEmail(String donorEmail, String donorName,
                                         String volunteerName, String pickupDate,
                                         String timeSlot) {
        sendEmail(donorEmail,
                "Pickup Confirmed",
                buildEmailLayout(
                        "Pickup Confirmed",
                        donorName,
                        "A volunteer has been assigned to your donation pickup.",
                        buildDetailsTable(new String[][]{
                                {"Volunteer", volunteerName},
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot}
                        }),
                        "#2e7d32"
                ));
    }

    public void sendDonationRequestDecisionEmail(String recipientEmail,
                                                 String recipientName,
                                                 String campaignName,
                                                 String decision,
                                                 String requestSummary,
                                                 String rejectionReason) {
        String details = buildDetailsTable(new String[][]{
                {"Campaign", campaignName},
                {"Decision", decision},
                {"Request", requestSummary},
                {"Reason", rejectionReason == null || rejectionReason.isBlank() ? "-" : rejectionReason}
        });

        sendEmail(recipientEmail,
                "Donation Request " + decision,
                buildEmailLayout(
                        "Donation Request " + decision,
                        recipientName,
                        "Your campaign donation request has been " + decision.toLowerCase() + ".",
                        details,
                        "approved".equalsIgnoreCase(decision) ? "#2e7d32" : "#c62828"
                ));
    }

    public void sendVolunteerTaskAcceptedAdminEmail(String recipientEmail,
                                                    String recipientName,
                                                    String volunteerName,
                                                    String donorName,
                                                    String pickupAddress,
                                                    String pickupDate,
                                                    String timeSlot,
                                                    String ngoName) {
        sendEmail(recipientEmail,
                "Volunteer Selected a Pickup Task",
                buildEmailLayout(
                        "Volunteer Task Selected",
                        recipientName,
                        volunteerName + " selected an approved pickup task.",
                        buildDetailsTable(new String[][]{
                                {"Volunteer", volunteerName},
                                {"Donor", donorName},
                                {"NGO", ngoName},
                                {"Pickup Address", pickupAddress},
                                {"Pickup Date", pickupDate},
                                {"Time Slot", timeSlot}
                        }),
                        "#1565c0"
                ));
    }

    private String buildEmailLayout(String headline,
                                    String recipientName,
                                    String intro,
                                    String bodyHtml,
                                    String accentColor) {
        String safeName = (recipientName == null || recipientName.isBlank())
                ? "there"
                : recipientName.trim();

        return """
                <div style="font-family:Arial,sans-serif;max-width:640px;margin:auto;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;background:#ffffff;">
                  <div style="background:%s;padding:20px 24px;">
                    <h2 style="color:#ffffff;margin:0;">%s</h2>
                  </div>
                  <div style="padding:24px;">
                    <p>Hello <strong>%s</strong>,</p>
                    <p>%s</p>
                    %s
                    <p style="margin-top:24px;color:#64748b;font-size:12px;">NGO Donation Management System</p>
                  </div>
                </div>
                """.formatted(accentColor, headline, safeName, intro, bodyHtml);
    }

    private String buildDetailsTable(String[][] rows) {
        StringBuilder html = new StringBuilder(
                "<table style=\"width:100%;border-collapse:collapse;margin:18px 0;\">");

        for (int index = 0; index < rows.length; index++) {
            String[] row = rows[index];
            String background = index % 2 == 0 ? "#f8fafc" : "#ffffff";
            html.append("""
                    <tr style="background:%s;">
                      <td style="padding:10px;border:1px solid #e5e7eb;font-weight:700;width:34%%;">%s</td>
                      <td style="padding:10px;border:1px solid #e5e7eb;">%s</td>
                    </tr>
                    """.formatted(background, row[0], valueOrDash(row[1])));
        }

        html.append("</table>");
        return html.toString();
    }

    private String buildActionLink(String label, String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return "";
        }

        return """
                <div style="margin-top:18px;">
                  <a href="%s" style="display:inline-block;padding:12px 16px;background:#ff6b35;color:#ffffff;text-decoration:none;border-radius:10px;font-weight:700;">%s</a>
                </div>
                """.formatted(actionUrl, label);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeFileName(String source, String fallback) {
        String value = valueOrDash(source).replaceAll("[^A-Za-z0-9._-]", "-");
        if (value.equals("-")) {
            return fallback;
        }
        return value;
    }

    public record EmailAttachment(String fileName, byte[] content, String contentType) {
    }
}
