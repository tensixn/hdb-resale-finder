package com.abcde.hdbresale.domain;

/** Value object: one estimated housing grant (EHG or PHG) and why that amount applies (FR-11). */
public record CpfGrant(String scheme, long amount, String reason) {
}
