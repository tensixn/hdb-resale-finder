package com.abcde.hdbresale.gateway;

/** One row of the data.gov.sg "Resale flat prices" dataset, reduced to the fields the system uses. */
public record ResaleRecord(String month, String town, String flatType, String block, String street,
                           String storeyRange, double floorAreaSqm, int leaseCommenceYear, long resalePrice) {
}
