package com.abcde.hdbresale.domain;

import jakarta.persistence.*;

/** Reference data: an HDB town with its centre point and NEA region (used for PSI readings). */
@Entity
@Table(name = "town")
public class Town {

    @Id
    private String name;
    private double latitude;
    private double longitude;
    private String region;

    protected Town() {
    }

    public Town(String name, double latitude, double longitude, String region) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getRegion() { return region; }
}
