package com.abcde.hdbresale.domain;

/** Marital status used for CPF grant eligibility (FR-10). Engaged couples apply under the fiance/fiancee scheme. */
public enum MaritalStatus {
    SINGLE, MARRIED, ENGAGED;

    public boolean isFamilyNucleus() {
        return this != SINGLE;
    }
}
