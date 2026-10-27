package com.abcde.hdbresale.domain;

import jakarta.persistence.*;

/** Entity: residential density (residents per hectare) for a town (URA/SingStat, loaded from CSV per A-5). */
@Entity
@Table(name = "density_record")
public class DensityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String town;
    @Column(name = "residents_per_ha")
    private double residentsPerHa;
    @Column(name = "record_year")
    private int year;

    protected DensityRecord() {
    }

    public DensityRecord(String town, double residentsPerHa, int year) {
        this.town = town;
        this.residentsPerHa = residentsPerHa;
        this.year = year;
    }

    public String getTown() { return town; }
    public double getResidentsPerHa() { return residentsPerHa; }
    public int getYear() { return year; }
}
