package com.abcde.hdbresale.service.strategy;

/**
 * The four normalised factor scores for one flat, each 0-100 where higher is better. Crime, density and PSI
 * are inverted before they get here (FR-7).
 */
public record LivabilityFactors(double lease, double safety, double density, double airQuality) {
}
