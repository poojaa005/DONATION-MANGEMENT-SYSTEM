package com.ngo.donation_management.service;

import com.ngo.donation_management.dto.LocationDTO;
import com.ngo.donation_management.dto.MapRouteDTO;
import com.ngo.donation_management.entity.DonorLocation;
import com.ngo.donation_management.entity.Donation;
import com.ngo.donation_management.entity.Ngo;
import com.ngo.donation_management.entity.PickupRequest;
import com.ngo.donation_management.entity.TaskAssignment;
import com.ngo.donation_management.entity.User;
import com.ngo.donation_management.entity.Volunteer;
import com.ngo.donation_management.repository.DonorLocationRepository;
import com.ngo.donation_management.repository.PickupRequestRepository;
import com.ngo.donation_management.repository.TaskAssignmentRepository;
import com.ngo.donation_management.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    @Autowired
    private DonorLocationRepository donorLocationRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private AccessScopeService accessScopeService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;

    /**
     * Save or update a donor's location for a pickup request.
     */
    public DonorLocation saveLocation(Integer pickupId, Double lat, Double lng,
                                       String addressLabel, String landmark) {
        PickupRequest pickup = pickupRequestRepository.findById(pickupId)
                .orElseThrow(() -> new RuntimeException("Pickup not found: " + pickupId));
        ensurePickupLocationWriteAccess(pickup);

        DonorLocation location = donorLocationRepository
                .findByPickupRequest_PickupId(pickupId)
                .orElse(new DonorLocation());

        location.setPickupRequest(pickup);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setAddressLabel(addressLabel);
        location.setLandmark(landmark);

        return donorLocationRepository.save(location);
    }

    /**
     * Get donor location for a specific pickup.
     */
    public DonorLocation getLocationByPickupId(Integer pickupId) {
        PickupRequest pickup = pickupRequestRepository.findById(pickupId)
                .orElseThrow(() -> new RuntimeException("Pickup not found: " + pickupId));
        ensurePickupLocationReadAccess(pickup);
        return donorLocationRepository.findByPickupRequest_PickupId(pickupId)
                .orElseThrow(() -> new RuntimeException(
                        "Location not found for pickup: " + pickupId));
    }

    /**
     * Get all pending pickup locations for the volunteer map view.
     */
    public List<LocationDTO> getAllPendingPickupLocations() {
        User currentUser = accessScopeService.requireCurrentUser();
        if (!accessScopeService.isVolunteer(currentUser)
                && !accessScopeService.isAdminLike(currentUser)) {
            throw new AccessDeniedException("Access denied");
        }

        List<PickupRequest> pickups = pickupRequestRepository
                .findByPickupStatus(PickupRequest.PickupStatus.pending)
                .stream()
                .filter(pickup -> taskAssignmentRepository
                        .findByPickupRequest_PickupId(pickup.getPickupId()).isEmpty())
                .toList();

        return filterPendingPickupsForCurrentVolunteer(pickups).stream()
                .map(this::buildLocationDto)
                .collect(Collectors.toList());
    }

    /**
     * Get locations assigned to a specific volunteer.
     */
    public List<LocationDTO> getLocationsForVolunteer(Integer volunteerId) {
        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException(
                        "Volunteer not found with id: " + volunteerId));
        ensureVolunteerLocationAccess(volunteer);
        return taskAssignmentRepository.findByVolunteer_VolunteerId(volunteerId).stream()
                .map(TaskAssignment::getPickupRequest)
                .map(this::buildLocationDto)
                .collect(Collectors.toList());
    }

    private LocationDTO buildLocationDto(PickupRequest pickup) {
        Optional<DonorLocation> location = donorLocationRepository
                .findByPickupRequest_PickupId(pickup.getPickupId());

        String ngoName = "Direct donation";
        Integer ngoId = null;

        Ngo ngo = resolveNgo(pickup.getDonation());
        if (ngo != null) {
            ngoId = ngo.getNgoId();
            ngoName = ngo.getNgoName();
        }

        return new LocationDTO(
                pickup.getPickupId(),
                pickup.getDonorAddress(),
                location.map(DonorLocation::getLatitude).orElse(null),
                location.map(DonorLocation::getLongitude).orElse(null),
                location.map(DonorLocation::getAddressLabel).orElse(pickup.getDonorAddress()),
                location.map(DonorLocation::getLandmark).orElse(""),
                pickup.getPickupDate() != null ? pickup.getPickupDate().toString() : "",
                pickup.getTimeSlot(),
                pickup.getPickupStatus().name(),
                pickup.getDonation() != null && pickup.getDonation().getUser() != null
                        ? pickup.getDonation().getUser().getName()
                        : "Donor",
                pickup.getDonorPhone(),
                ngoId,
                ngoName
        );
    }

    /**
     * Build a Google Maps directions URL for navigation.
     */
    public MapRouteDTO getNavigationRoute(Double fromLat, Double fromLng,
                                           Double toLat, Double toLng) {
        String googleMapsUrl = String.format(
                "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                fromLat, fromLng, toLat, toLng);

        String embedUrl = String.format(
                "https://www.google.com/maps?output=embed&saddr=%s&daddr=%s",
                URLEncoder.encode(String.format("%f,%f", fromLat, fromLng), StandardCharsets.UTF_8),
                URLEncoder.encode(String.format("%f,%f", toLat, toLng), StandardCharsets.UTF_8));

        return new MapRouteDTO(googleMapsUrl, embedUrl, fromLat, fromLng, toLat, toLng);
    }

    public String getGoogleMapsApiKey() {
        return googleMapsApiKey;
    }

    private List<PickupRequest> filterPendingPickupsForCurrentVolunteer(List<PickupRequest> pickups) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return pickups;
        }

        if (accessScopeService.isNgoAdmin(currentUser)) {
            Integer currentNgoId = accessScopeService.requireNgoId(currentUser);
            return pickups.stream()
                    .filter(pickup -> Objects.equals(resolvePickupNgoId(pickup), currentNgoId))
                    .toList();
        }

        if (!accessScopeService.isVolunteer(currentUser)) {
            throw new AccessDeniedException("Access denied");
        }

        volunteerRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Volunteer profile not found for the current user"));

        return pickups;
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

    private Integer resolvePickupNgoId(PickupRequest pickup) {
        Ngo ngo = resolveNgo(pickup != null ? pickup.getDonation() : null);
        return ngo != null ? ngo.getNgoId() : null;
    }

    private void ensurePickupLocationWriteAccess(PickupRequest pickup) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer donorUserId = pickup.getDonation() != null
                    && pickup.getDonation().getUser() != null
                    ? pickup.getDonation().getUser().getUserId()
                    : null;
            if (!Objects.equals(currentUser.getUserId(), donorUserId)) {
                throw new AccessDeniedException(
                        "You can only save locations for your own pickups");
            }
            return;
        }

        Integer pickupNgoId = resolvePickupNgoId(pickup);
        if (pickupNgoId != null) {
            accessScopeService.ensureNgoAccess(pickupNgoId);
            return;
        }

        if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new AccessDeniedException(
                    "NGO admins can only manage pickup locations for their NGO");
        }

        throw new AccessDeniedException("Access denied");
    }

    private void ensurePickupLocationReadAccess(PickupRequest pickup) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isDonor(currentUser)) {
            Integer donorUserId = pickup.getDonation() != null
                    && pickup.getDonation().getUser() != null
                    ? pickup.getDonation().getUser().getUserId()
                    : null;
            if (!Objects.equals(currentUser.getUserId(), donorUserId)) {
                throw new AccessDeniedException(
                        "You can only view locations for your own pickups");
            }
            return;
        }

        if (accessScopeService.isVolunteer(currentUser)) {
            boolean assignedToCurrentVolunteer = taskAssignmentRepository
                    .findByPickupRequest_PickupId(pickup.getPickupId()).stream()
                    .anyMatch(task -> task.getVolunteer() != null
                            && task.getVolunteer().getUser() != null
                            && Objects.equals(
                                    task.getVolunteer().getUser().getUserId(),
                                    currentUser.getUserId()));
            if (!assignedToCurrentVolunteer) {
                throw new AccessDeniedException(
                        "You can only view locations for your assigned pickups");
            }
            return;
        }

        Integer pickupNgoId = resolvePickupNgoId(pickup);
        if (pickupNgoId != null) {
            accessScopeService.ensureNgoAccess(pickupNgoId);
            return;
        }

        if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new AccessDeniedException(
                    "NGO admins can only view pickup locations for their NGO");
        }

        throw new AccessDeniedException("Access denied");
    }

    private void ensureVolunteerLocationAccess(Volunteer volunteer) {
        User currentUser = accessScopeService.requireCurrentUser();
        if (accessScopeService.isAppAdmin(currentUser)) {
            return;
        }

        if (accessScopeService.isVolunteer(currentUser)) {
            if (volunteer.getUser() == null
                    || !Objects.equals(
                            volunteer.getUser().getUserId(),
                            currentUser.getUserId())) {
                throw new AccessDeniedException(
                        "You can only view locations for your own volunteer account");
            }
            return;
        }

        Integer volunteerNgoId = volunteer.getNgo() != null
                ? volunteer.getNgo().getNgoId()
                : null;
        if (volunteerNgoId != null) {
            accessScopeService.ensureNgoAccess(volunteerNgoId);
            return;
        }

        if (accessScopeService.getScopedNgoId().isPresent()) {
            throw new AccessDeniedException(
                    "NGO admins can only view volunteer locations for their NGO");
        }

        throw new AccessDeniedException("Access denied");
    }

}
