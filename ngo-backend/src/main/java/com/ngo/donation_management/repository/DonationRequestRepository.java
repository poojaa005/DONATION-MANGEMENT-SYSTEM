package com.ngo.donation_management.repository;

// repository/DonationRequestRepository.java

import com.ngo.donation_management.entity.DonationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRequestRepository
        extends JpaRepository<DonationRequest, Integer> {
    List<DonationRequest> findByUser_UserId(Integer userId);
    List<DonationRequest> findByApprovedBy_UserId(Integer userId);
    List<DonationRequest> findByCampaign_CampaignId(
            Integer campaignId);
    List<DonationRequest> findByCampaign_Ngo_NgoId(Integer ngoId);
    List<DonationRequest> findByRequestStatus(
            DonationRequest.RequestStatus status);
    List<DonationRequest> findByRequestStatusAndCampaign_Ngo_NgoId(
            DonationRequest.RequestStatus status, Integer ngoId);
    List<DonationRequest> findByDonationType(
            DonationRequest.DonationType type);
    List<DonationRequest> findByDonationTypeAndCampaign_Ngo_NgoId(
            DonationRequest.DonationType type, Integer ngoId);
}
