package com.abcde.hdbresale.domain;

/** Value object: a commute anchor resolved to coordinates (Data Dictionary: Workplace). */
public record Workplace(String query, String label, GeoPoint location) {
}
