package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Year;

/**
 * Entity: an HDB resale flat. There is no public listings API (assumption A-4), so a flat is a unit that
 * appears in the resale transaction data, and its "price" is its most recent transaction price.
 */
@Entity
@Table(name = "flat")
public class Flat {

    public static final int LEASE_YEARS = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String block;
    private String street;
    private String town;
    @Column(name = "flat_type")
    private String flatType;
    @Column(name = "floor_area_sqm")
    private double floorAreaSqm;
    @Column(name = "storey_range")
    private String storeyRange;
    @Column(name = "lease_commence_year")
    private int leaseCommenceYear;
    private double latitude;
    private double longitude;
    private String region;
    @Column(name = "latest_price")
    private Long latestPrice;
    @Column(name = "latest_month")
    private String latestMonth;

    protected Flat() {
    }

    public Flat(String block, String street, String town, String flatType, double floorAreaSqm, String storeyRange,
                int leaseCommenceYear, double latitude, double longitude, String region) {
        this.block = block;
        this.street = street;
        this.town = town;
        this.flatType = flatType;
        this.floorAreaSqm = floorAreaSqm;
        this.storeyRange = storeyRange;
        this.leaseCommenceYear = leaseCommenceYear;
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
    }

    /** Whole years of lease left this year. */
    public int remainingLeaseYears() {
        return Math.max(0, LEASE_YEARS - (Year.now().getValue() - leaseCommenceYear));
    }

    public String address() {
        return block + " " + street;
    }

    public GeoPoint location() {
        return new GeoPoint(latitude, longitude);
    }

    /** Records a transaction as the latest price if it is newer than the one held. */
    public void applyTransaction(String month, long price) {
        if (latestMonth == null || month.compareTo(latestMonth) >= 0) {
            latestMonth = month;
            latestPrice = price;
        }
    }

    public Long getId() { return id; }
    public String getBlock() { return block; }
    public String getStreet() { return street; }
    public String getTown() { return town; }
    public String getFlatType() { return flatType; }
    public double getFloorAreaSqm() { return floorAreaSqm; }
    public String getStoreyRange() { return storeyRange; }
    public int getLeaseCommenceYear() { return leaseCommenceYear; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getRegion() { return region; }
    public Long getLatestPrice() { return latestPrice; }
    public String getLatestMonth() { return latestMonth; }
}
