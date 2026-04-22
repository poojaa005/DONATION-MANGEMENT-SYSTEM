package com.ngo.donation_management.controller;

// controller/ReportController.java
import com.ngo.donation_management.dto.ReportDTO;
import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.CampaignRepository;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.PaymentRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import com.ngo.donation_management.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private PickupRequestService pickupRequestService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private ReportDTO buildReport() {
        ReportDTO report = new ReportDTO();
        List<Donation> scopedDonations = donationService.getAllDonations();

        report.setTotalDonations(
                scopedDonations.size());
        report.setTotalAmountCollected(
                scopedDonations.stream()
                        .filter(donation -> donation.getDonationStatus()
                                == Donation.DonationStatus.completed)
                        .map(Donation::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setTotalCampaigns(
                campaignService.getAllCampaigns().size());
        report.setActiveCampaigns(
                campaignService.countByStatus("active"));
        report.setCompletedCampaigns(
                campaignService.countByStatus("completed"));
        report.setTotalVolunteers(
                volunteerService.getAllVolunteers().size());
        report.setActiveVolunteers(
                volunteerService.countActiveVolunteers());
        report.setTotalDonors(
                (int) scopedDonations.stream()
                        .map(Donation::getUser)
                        .filter(user -> user != null)
                        .map(user -> user.getUserId())
                        .distinct()
                        .count());
        report.setPendingPickups(
                pickupRequestService.countByStatus("pending"));
        report.setCompletedPickups(
                pickupRequestService.countByStatus("completed"));
        report.setTotalPayments(
                paymentService.getAllPayments().size());
        report.setTotalPaymentAmount(
                paymentService.getTotalSuccessfulAmount());
        report.setGoodsDonations(
                donationService.getByType("goods").size());

        return report;
    }

    private ReportDTO buildPublicReport() {
        ReportDTO report = new ReportDTO();
        report.setTotalDonations(donationRepository.count());
        report.setTotalAmountCollected(donationRepository.getTotalCompletedAmount());
        report.setTotalCampaigns(campaignRepository.count());
        report.setActiveCampaigns(
                campaignRepository.countByCampaignStatus(Campaign.CampaignStatus.active));
        report.setCompletedCampaigns(
                campaignRepository.countByCampaignStatus(Campaign.CampaignStatus.completed));
        report.setTotalVolunteers(volunteerRepository.count());
        report.setActiveVolunteers(
                volunteerRepository.countByVolunteerStatus(Volunteer.VolunteerStatus.active));
        report.setTotalDonors(donationRepository.countDistinctDonors());
        report.setPendingPickups(
                pickupRequestRepository.countByPickupStatus(PickupRequest.PickupStatus.pending));
        report.setCompletedPickups(
                pickupRequestRepository.countByPickupStatus(PickupRequest.PickupStatus.completed));
        report.setTotalPayments(paymentRepository.count());
        report.setTotalPaymentAmount(paymentRepository.getTotalSuccessfulPaymentAmount());
        report.setGoodsDonations(
                donationRepository.countByDonationType(Donation.DonationType.goods));

        return report;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGO_ADMIN')")
    public ResponseEntity<ReportDTO> getDashboardReport() {
        return ResponseEntity.ok(buildReport());
    }

    @GetMapping("/public-summary")
    public ResponseEntity<ReportDTO> getPublicSummary() {
        return ResponseEntity.ok(buildPublicReport());
    }
}
