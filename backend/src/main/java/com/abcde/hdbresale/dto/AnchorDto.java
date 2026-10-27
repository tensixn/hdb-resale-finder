package com.abcde.hdbresale.dto;

/** A resolved workplace anchor. {@code approximate} is true when it came from the offline gazetteer. */
public record AnchorDto(int index, String query, String label, double latitude, double longitude, boolean approximate) {
}
