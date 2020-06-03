package com.example.contact_tracing.models;

public class LocationTimestamp {
    public String userId;
    public Double lat;
    public Double lon;
    public Long timestamp;

    public LocationTimestamp(String userId, double lat, double lon, Long timestamp) {
        this.userId = userId;
        this.lat = lat;
        this.lon = lon;
        this.timestamp = timestamp;
    }

}
