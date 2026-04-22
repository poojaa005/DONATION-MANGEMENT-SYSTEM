package com.ngo.donation_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores the lat/lng of a donor for a specific pickup request.
 * The volunteer uses this to navigate to the donor's location via Google Maps.
 */
@Entity
@Table(name = "donor_location")
public class DonorLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Integer locationId;

    @OneToOne
    @JoinColumn(name = "pickup_id", nullable = false, unique = true)
    private PickupRequest pickupRequest;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "address_label", length = 300)
    private String addressLabel;

    @Column(name = "landmark", length = 200)
    private String landmark;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DonorLocation() {}

    public DonorLocation(PickupRequest pickupRequest, Double latitude,
                          Double longitude, String addressLabel, String landmark) {
        this.pickupRequest = pickupRequest;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressLabel = addressLabel;
        this.landmark = landmark;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) { this.locationId = locationId; }

    public PickupRequest getPickupRequest() { return pickupRequest; }
    public void setPickupRequest(PickupRequest pickupRequest) { this.pickupRequest = pickupRequest; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddressLabel() { return addressLabel; }
    public void setAddressLabel(String addressLabel) { this.addressLabel = addressLabel; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
