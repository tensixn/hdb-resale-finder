package com.abcde.hdbresale.domain;

import jakarta.persistence.*;

/** Entity: reported crime rate per 1,000 residents for a town (SPF, loaded from CSV per assumption A-5). */
@Entity
@Table(name = "crime_record")
public class CrimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String town;
    @Column(name = "rate_per_thousand")
    private double ratePerThousand;
    @Column(name = "record_year")
    private int year;

    protected CrimeRecord() {
    }

    public CrimeRecord(String town, double ratePerThousand, int year) {
        this.town = town;
        this.ratePerThousand = ratePerThousand;
        this.year = year;
    }

    public String getTown() { return town; }
    public double getRatePerThousand() { return ratePerThousand; }
    public int getYear() { return year; }
}
