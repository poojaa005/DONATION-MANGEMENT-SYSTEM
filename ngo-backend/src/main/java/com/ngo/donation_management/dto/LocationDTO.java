package com.ngo.donation_management.dto;

public class LocationDTO {
    private Integer pickupId;
    private String address;
    private Double latitude;
    private Double longitude;
    private String addressLabel;
    private String landmark;
    private String pickupDate;
    private String timeSlot;
    private String status;
    private String donorName;
    private String donorPhone;
    private Integer ngoId;
    private String ngoName;

    public LocationDTO() {}

    public LocationDTO(Integer pickupId, String address, Double latitude, Double longitude,
                        String addressLabel, String landmark, String pickupDate,
                        String timeSlot, String status, String donorName,
                        String donorPhone,
                        Integer ngoId, String ngoName) {
        this.pickupId = pickupId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressLabel = addressLabel;
        this.landmark = landmark;
        this.pickupDate = pickupDate;
        this.timeSlot = timeSlot;
        this.status = status;
        this.donorName = donorName;
        this.donorPhone = donorPhone;
        this.ngoId = ngoId;
        this.ngoName = ngoName;
    }

    // Getters and Setters
    public Integer getPickupId() { return pickupId; }
    public void setPickupId(Integer pickupId) { this.pickupId = pickupId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddressLabel() { return addressLabel; }
    public void setAddressLabel(String addressLabel) { this.addressLabel = addressLabel; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getPickupDate() { return pickupDate; }
    public void setPickupDate(String pickupDate) { this.pickupDate = pickupDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getDonorPhone() { return donorPhone; }
    public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }

    public Integer getNgoId() { return ngoId; }
    public void setNgoId(Integer ngoId) { this.ngoId = ngoId; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }
}
