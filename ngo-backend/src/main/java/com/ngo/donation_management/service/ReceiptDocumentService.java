package com.ngo.donation_management.service;

import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.DonationReceipt;
import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.entity.Payment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReceiptDocumentService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] generateReceiptPdf(Donation donation,
                                     Payment payment,
                                     DonationReceipt receipt) {
        Ngo ngo = resolveNgo(donation);
        List<String> lines = List.of(
                "Official Donation Receipt",
                "",
                "Receipt Number: " + valueOrDash(receipt != null ? receipt.getReceiptNumber() : null),
                "Donation ID: #" + valueOrDash(donation != null ? donation.getDonationId() : null),
                "Donor Name: " + valueOrDash(donation != null && donation.getUser() != null ? donation.getUser().getName() : null),
                "Donor Email: " + valueOrDash(donation != null && donation.getUser() != null ? donation.getUser().getEmail() : null),
                "Campaign / NGO: " + valueOrDash(donation != null && donation.getCampaign() != null
                        ? donation.getCampaign().getTitle()
                        : ngo != null ? ngo.getNgoName() : "Direct donation"),
                "Amount: " + formatAmount(payment != null ? payment.getAmount() : donation != null ? donation.getAmount() : null),
                "Payment Method: " + valueOrDash(payment != null && payment.getPaymentMethod() != null
                        ? payment.getPaymentMethod().name().replace('_', ' ')
                        : null),
                "Transaction ID: " + valueOrDash(payment != null ? payment.getTransactionId() : null),
                "Issued On: " + formatDateTime(receipt != null ? receipt.getIssuedDate() : null),
                "",
                "This document confirms that the donation was received successfully."
        );

        return renderPdf("Donation Receipt", lines);
    }

    public byte[] generateCertificatePdf(Donation donation,
                                         DonationReceipt receipt) {
        Ngo ngo = resolveNgo(donation);
        String donorName = donation != null && donation.getUser() != null
                ? valueOrDash(donation.getUser().getName())
                : "Donor";
        String ngoName = ngo != null ? ngo.getNgoName() : "NGO Donation System";
        String amount = formatAmount(donation != null ? donation.getAmount() : null);

        List<String> lines = new ArrayList<>();
        lines.add("Certificate of Appreciation");
        lines.add("");
        lines.add("This certificate is proudly presented to");
        lines.add(donorName);
        lines.add("");
        lines.add("for the generous donation of " + amount);
        lines.add("in support of " + ngoName + ".");
        lines.add("");
        lines.add("Certificate Reference: " + valueOrDash(receipt != null ? receipt.getReceiptNumber() : null));
        lines.add("Issued On: " + formatDateTime(receipt != null ? receipt.getIssuedDate() : null));
        lines.add("");
        lines.add("Thank you for creating meaningful impact.");

        return renderPdf("Donation Certificate", lines);
    }

    private byte[] renderPdf(String title,
                             List<String> lines) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                contentStream.newLineAtOffset(50, 780);
                contentStream.showText(title);
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 740);

                for (String line : lines) {
                    contentStream.showText(sanitize(line));
                    contentStream.newLineAtOffset(0, -20);
                }
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to generate receipt PDF", exception);
        }
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

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "Rupees 0.00";
        }

        return "Rupees " + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", " ");
    }
}
