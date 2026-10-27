// TypeScript interfaces mirroring backend DTOs / domain entities (Appendix A Data Dictionary).

export interface Flat {
  // TODO: flatId, block, street, town, flatType, leaseRemaining, floorArea, latitude, longitude
}

export interface LivabilityIndex {
  // TODO: leaseScore, crimeScore, densityScore, airQualityScore, weightedTotal
}

export interface ValuationLabel {
  // TODO: label, estateAveragePrice
}

export interface CpfGrant {
  // TODO: ehgAmount, phgAmount, monthlyOutlay
}

export interface Bookmark {
  // TODO: userId, flatId, savedDate
}
