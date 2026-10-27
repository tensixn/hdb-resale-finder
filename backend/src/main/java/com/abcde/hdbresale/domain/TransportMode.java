package com.abcde.hdbresale.domain;

/** Transport modes a Home Buyer can choose for commute calculation (FR-2). */
public enum TransportMode {
    PUBLIC_TRANSPORT("Public transport"),
    DRIVE("Drive"),
    CYCLE("Cycle"),
    WALK("Walk");

    private final String label;

    TransportMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
