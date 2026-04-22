package com.ngo.donation_management.service;

// service/CampaignService.java

import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.repository.CampaignRepository;
import com.ngo.donation_management.repository.DonationRepository;
import com.ngo.donation_management.repository.DonationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationRequestRepository donationRequestRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    public Campaign createCampaign(Campaign campaign) {
        User currentUser = accessScopeService.requireCurrentUser();
        Integer ngoId = campaign.getNgo() != null
                ? campaign.getNgo().getNgoId()
                : null;

        if (ngoId == null) {
            throw new IllegalArgumentException("Campaign NGO is required");
        }

        accessScopeService.ensureNgoAccess(ngoId);
        campaign.setAdmin(currentUser);
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isPresent()) {
            return campaignRepository.findByNgo_NgoId(scopedNgoId.get());
        }
        return campaignRepository.findAll();
    }

    public Optional<Campaign> getCampaignById(Integer id) {
        return campaignRepository.findById(id)
                .filter(this::canAccessCampaign);
    }

    public List<Campaign> getByStatus(String status) {
        List<Campaign> campaigns = campaignRepository.findByCampaignStatus(
                Campaign.CampaignStatus.valueOf(status.toLowerCase()));
        return filterCampaignsByScope(campaigns);
    }

    public List<Campaign> getByNgoId(Integer ngoId) {
        accessScopeService.ensureNgoAccess(ngoId);
        return campaignRepository.findByNgo_NgoId(ngoId);
    }

    public List<Campaign> searchByTitle(String title) {
        return filterCampaignsByScope(campaignRepository
                .findByTitleContainingIgnoreCase(title));
    }

    public List<Campaign> getByDonationType(String type) {
        return filterCampaignsByScope(campaignRepository.findByDonationType(
                Campaign.DonationType.valueOf(type.toLowerCase())));
    }

    public Campaign updateCampaign(Integer id,
                                   Campaign updatedCampaign) {
        return campaignRepository.findById(id).map(campaign -> {
            Integer currentNgoId = campaign.getNgo() != null
                    ? campaign.getNgo().getNgoId()
                    : null;
            if (currentNgoId != null) {
                accessScopeService.ensureNgoAccess(currentNgoId);
            }

            User currentUser = accessScopeService.requireCurrentUser();
            Integer updatedNgoId = updatedCampaign.getNgo() != null
                    ? updatedCampaign.getNgo().getNgoId()
                    : null;
            if (updatedNgoId == null) {
                throw new IllegalArgumentException("Campaign NGO is required");
            }

            accessScopeService.ensureNgoAccess(updatedNgoId);
            campaign.setNgo(updatedCampaign.getNgo());
            campaign.setAdmin(currentUser);
            campaign.setTitle(updatedCampaign.getTitle());
            campaign.setDescription(
                    updatedCampaign.getDescription());
            campaign.setDonationType(
                    updatedCampaign.getDonationType());
            campaign.setTargetAmount(
                    updatedCampaign.getTargetAmount());
            campaign.setCollectedAmount(
                    updatedCampaign.getCollectedAmount());
            campaign.setStartDate(updatedCampaign.getStartDate());
            campaign.setEndDate(updatedCampaign.getEndDate());
            campaign.setCampaignStatus(
                    updatedCampaign.getCampaignStatus());
            return campaignRepository.save(campaign);
        }).orElseThrow(() -> new RuntimeException(
                "Campaign not found with id: " + id));
    }

    @Transactional
    public void deleteCampaign(Integer id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Campaign not found with id: " + id));
        if (campaign.getNgo() != null) {
            accessScopeService.ensureNgoAccess(campaign.getNgo().getNgoId());
        }

        donationRequestRepository.deleteAll(
                donationRequestRepository.findByCampaign_CampaignId(id));

        List<Donation> donations =
                donationRepository.findByCampaign_CampaignId(id);
        donations.forEach(donation -> donation.setCampaign(null));
        donationRepository.saveAll(donations);

        campaignRepository.delete(campaign);
    }

    public long countByStatus(String status) {
        return getByStatus(status).size();
    }

    private List<Campaign> filterCampaignsByScope(List<Campaign> campaigns) {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isEmpty()) {
            return campaigns;
        }

        Integer ngoId = scopedNgoId.get();
        return campaigns.stream()
                .filter(campaign -> campaign.getNgo() != null
                        && ngoId.equals(campaign.getNgo().getNgoId()))
                .collect(Collectors.toList());
    }

    private boolean canAccessCampaign(Campaign campaign) {
        Optional<Integer> scopedNgoId = accessScopeService.getScopedNgoId();
        if (scopedNgoId.isEmpty()) {
            return true;
        }

        return campaign != null
                && campaign.getNgo() != null
                && scopedNgoId.get().equals(campaign.getNgo().getNgoId());
    }
}
