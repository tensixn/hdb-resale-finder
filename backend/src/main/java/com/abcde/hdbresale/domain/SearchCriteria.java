package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Entity: a signed-in user's most recent search, so the Search screen can be pre-filled next time. */
@Entity
@Table(name = "search_criteria")
public class SearchCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    private String workplace1;
    private String workplace2;
    @Column(name = "transport_modes")
    private String transportModes;
    @Column(name = "min_price")
    private Long minPrice;
    @Column(name = "max_price")
    private Long maxPrice;
    @Column(name = "flat_types")
    private String flatTypes;
    private String towns;
    @Column(name = "created_at")
    private Instant createdAt;

    protected SearchCriteria() {
    }

    public SearchCriteria(Long userId, String workplace1, String workplace2, String transportModes, Long minPrice,
                          Long maxPrice, String flatTypes, String towns, Instant createdAt) {
        this.userId = userId;
        this.workplace1 = workplace1;
        this.workplace2 = workplace2;
        this.transportModes = transportModes;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.flatTypes = flatTypes;
        this.towns = towns;
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public String getWorkplace1() { return workplace1; }
    public String getWorkplace2() { return workplace2; }
    public String getTransportModes() { return transportModes; }
    public Long getMinPrice() { return minPrice; }
    public Long getMaxPrice() { return maxPrice; }
    public String getFlatTypes() { return flatTypes; }
    public String getTowns() { return towns; }
    public Instant getCreatedAt() { return createdAt; }
}
