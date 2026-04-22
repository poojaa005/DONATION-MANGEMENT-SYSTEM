package com.ngo.donation_management.controller;

// controller/CampaignController.java

import com.ngo.donation_management.entity.Campaign;
import com.ngo.donation_management.service.CampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    // CREATE
    @PostMapping
    public ResponseEntity<Campaign> createCampaign(
            @RequestBody Campaign campaign) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.createCampaign(campaign));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        return ResponseEntity.ok(
                campaignService.getAllCampaigns());
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(
            @PathVariable Integer id) {
        return campaignService.getCampaignById(id)
                .map(c -> ResponseEntity.ok((Object) c))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error",
                                "Campaign not found with id: "
                                        + id)));
    }

    // SEARCH BY STATUS
    @GetMapping("/search/status")
    public ResponseEntity<List<Campaign>> getByStatus(
            @RequestParam String status) {
        return ResponseEntity.ok(
                campaignService.getByStatus(status));
    }

    // SEARCH BY NGO ID
    @GetMapping("/search/ngo/{ngoId}")
    public ResponseEntity<List<Campaign>> getByNgoId(
            @PathVariable Integer ngoId) {
        return ResponseEntity.ok(
                campaignService.getByNgoId(ngoId));
    }

    // SEARCH BY TITLE
    @GetMapping("/search/title")
    public ResponseEntity<List<Campaign>> searchByTitle(
            @RequestParam String title) {
        return ResponseEntity.ok(
                campaignService.searchByTitle(title));
    }

    // SEARCH BY DONATION TYPE
    @GetMapping("/search/type")
    public ResponseEntity<List<Campaign>> getByDonationType(
            @RequestParam String type) {
        return ResponseEntity.ok(
                campaignService.getByDonationType(type));
    }

    // GET ACTIVE CAMPAIGNS (Guest feature)
    @GetMapping("/active")
    public ResponseEntity<List<Campaign>> getActiveCampaigns() {
        return ResponseEntity.ok(
                campaignService.getByStatus("active"));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Campaign> updateCampaign(
            @PathVariable Integer id,
            @RequestBody Campaign campaign) {
        return ResponseEntity.ok(
                campaignService.updateCampaign(id, campaign));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCampaign(
            @PathVariable Integer id) {
        try {
            campaignService.deleteCampaign(id);
            return ResponseEntity.ok(Map.of("message",
                    "Campaign deleted successfully"));
        } catch (RuntimeException exception) {
            HttpStatus status = exception.getMessage() != null
                    && exception.getMessage().contains("Campaign not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
