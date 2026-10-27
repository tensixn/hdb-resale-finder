import { Hourglass, Leaf, Scale, ShieldCheck, type LucideIcon } from '../components/icons';
import type { Weights } from '../types/models';

export interface Preset {
  id: 'balanced' | 'lease' | 'green' | 'safe';
  label: string;
  blurb: string;
  icon: LucideIcon;
  weights: Weights;
}

/**
 * One-tap starting points for the four livability weights (each totals 100%). Commute is fixed at half of the
 * match score, so there is deliberately no "short commute" preset: it could only be faked by hiding flats.
 */
export const PRESETS: Preset[] = [
  { id: 'balanced', label: 'Balanced', blurb: 'All four factors count equally.', icon: Scale, weights: { lease: 25, safety: 25, density: 25, airQuality: 25 } },
  { id: 'lease', label: 'Long lease', blurb: 'A flat that holds its value for decades.', icon: Hourglass, weights: { lease: 55, safety: 15, density: 15, airQuality: 15 } },
  { id: 'green', label: 'Quiet & green', blurb: 'Less crowded estates with cleaner air.', icon: Leaf, weights: { lease: 10, safety: 15, density: 45, airQuality: 30 } },
  { id: 'safe', label: 'Safe & sound', blurb: 'Towns with the lowest reported crime.', icon: ShieldCheck, weights: { lease: 15, safety: 55, density: 15, airQuality: 15 } },
];

const KEYS: (keyof Weights)[] = ['lease', 'safety', 'density', 'airQuality'];

/** The preset these weights match, or null once the user has fine-tuned them. */
export function activePreset(weights: Weights): Preset | null {
  return PRESETS.find((p) => KEYS.every((k) => Math.abs(p.weights[k] - weights[k]) < 0.5)) ?? null;
}
