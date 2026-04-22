package com.ngo.donation_management.dto;

public class MapRouteDTO {
    private String googleMapsUrl;
    private String embedUrl;
    private Double fromLatitude;
    private Double fromLongitude;
    private Double toLatitude;
    private Double toLongitude;

    public MapRouteDTO() {}

    public MapRouteDTO(String googleMapsUrl, String embedUrl,
                        Double fromLatitude, Double fromLongitude,
                        Double toLatitude, Double toLongitude) {
        this.googleMapsUrl = googleMapsUrl;
        this.embedUrl = embedUrl;
        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
    }

    // Getters and Setters
    public String getGoogleMapsUrl() { return googleMapsUrl; }
    public void setGoogleMapsUrl(String googleMapsUrl) { this.googleMapsUrl = googleMapsUrl; }

    public String getEmbedUrl() { return embedUrl; }
    public void setEmbedUrl(String embedUrl) { this.embedUrl = embedUrl; }

    public Double getFromLatitude() { return fromLatitude; }
    public void setFromLatitude(Double fromLatitude) { this.fromLatitude = fromLatitude; }

    public Double getFromLongitude() { return fromLongitude; }
    public void setFromLongitude(Double fromLongitude) { this.fromLongitude = fromLongitude; }

    public Double getToLatitude() { return toLatitude; }
    public void setToLatitude(Double toLatitude) { this.toLatitude = toLatitude; }

    public Double getToLongitude() { return toLongitude; }
    public void setToLongitude(Double toLongitude) { this.toLongitude = toLongitude; }
}
