package com.abcde.hdbresale.domain;

import jakarta.persistence.*;

/** Entity: a primary school shown on the Flat Detail screen for information only (MOE, loaded from CSV). */
@Entity
@Table(name = "school")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String town;
    private double latitude;
    private double longitude;

    protected School() {
    }

    public School(String name, String town, double latitude, double longitude) {
        this.name = name;
        this.town = town;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() { return name; }
    public String getTown() { return town; }
    public GeoPoint location() { return new GeoPoint(latitude, longitude); }
}
