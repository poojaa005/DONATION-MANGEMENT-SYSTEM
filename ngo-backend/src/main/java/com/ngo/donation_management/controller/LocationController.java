package com.ngo.donation_management.controller;

import com.ngo.donation_management.dto.LocationDTO;
import com.ngo.donation_management.dto.MapRouteDTO;
import com.ngo.donation_management.entity.DonorLocation;
import com.ngo.donation_management.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @Autowired
    private LocationService locationService;

    /**
     * POST /api/locations/pickup/{pickupId}
     * Donor or admin saves geo-coordinates for a pickup request.
     * Body: { "latitude": 13.08, "longitude": 80.27, "addressLabel": "...", "landmark": "..." }
     */
    @PostMapping("/pickup/{pickupId}")
    public ResponseEntity<DonorLocation> saveLocation(
            @PathVariable Integer pickupId,
            @RequestBody Map<String, Object> body) {
        Double lat = Double.parseDouble(body.get("latitude").toString());
        Double lng = Double.parseDouble(body.get("longitude").toString());
        String label = body.getOrDefault("addressLabel", "").toString();
        String landmark = body.getOrDefault("landmark", "").toString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.saveLocation(pickupId, lat, lng, label, landmark));
    }

    /**
     * GET /api/locations/pickup/{pickupId}
     * Get the stored location for a specific pickup.
     */
    @GetMapping("/pickup/{pickupId}")
    public ResponseEntity<DonorLocation> getLocationByPickup(@PathVariable Integer pickupId) {
        return ResponseEntity.ok(locationService.getLocationByPickupId(pickupId));
    }

    /**
     * GET /api/locations/pending
     * Get all pending pickup locations — used by volunteers to see a map of available tasks.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<LocationDTO>> getPendingPickupLocations() {
        return ResponseEntity.ok(locationService.getAllPendingPickupLocations());
    }

    /**
     * GET /api/locations/volunteer/{volunteerId}
     * Get all locations assigned to a specific volunteer.
     */
    @GetMapping("/volunteer/{volunteerId}")
    public ResponseEntity<List<LocationDTO>> getLocationsForVolunteer(
            @PathVariable Integer volunteerId) {
        return ResponseEntity.ok(locationService.getLocationsForVolunteer(volunteerId));
    }

    /**
     * GET /api/locations/route?fromLat=&fromLng=&toLat=&toLng=
     * Generate Google Maps navigation URL from volunteer's current location to donor.
     */
    @GetMapping("/route")
    public ResponseEntity<MapRouteDTO> getRoute(
            @RequestParam Double fromLat,
            @RequestParam Double fromLng,
            @RequestParam Double toLat,
            @RequestParam Double toLng) {
        return ResponseEntity.ok(locationService.getNavigationRoute(fromLat, fromLng, toLat, toLng));
    }

    /**
     * GET /api/locations/maps-key
     * Returns the Google Maps API key for the frontend to render maps.
     */
    @GetMapping("/maps-key")
    public ResponseEntity<Map<String, String>> getMapsKey() {
        return ResponseEntity.ok(Map.of("apiKey", locationService.getGoogleMapsApiKey()));
    }
}
