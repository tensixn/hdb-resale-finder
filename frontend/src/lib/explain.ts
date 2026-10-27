import type { Anchor, RankedFlat, Weights } from '../types/models';

/**
 * Turns a ranked flat into the pieces of its match score so the UI can say *why* it ranks where it does.
 *
 * Match = 50% commute + 50% Livability Index.
 *   commute    = mean of the per-workplace leg scores, so each leg is worth 50 / legs points.
 *   livability = sum of factor score x weight, so each factor is worth 50 x weight / 100 points.
 * Every part therefore contributes `share x score` points and the parts add up to the match score.
 */

export type PartKey = 'w0' | 'w1' | 'lease' | 'safety' | 'density' | 'airQuality';

export interface Part {
  key: PartKey;
  kind: 'commute' | 'livability';
  label: string;
  /** Max points this part can give (its share of 100). */
  share: number;
  /** 0-100 score for this part. */
  score: number;
  /** share x score / 100: what it actually adds to the match score. */
  points: number;
  /** A human value: "44 min", "86 yrs left", "score 84". */
  raw: string;
  minutes?: number;
  years?: number;
}

export const PART_COLOR: Record<PartKey, string> = {
  w0: '#334155',
  w1: '#a8472f',
  lease: '#1d7a6b',
  safety: '#6b7f3a',
  density: '#d98a2b',
  airQuality: '#2b9fc9',
};

export const FACTOR_SHORT: Record<keyof Weights, string> = {
  lease: 'Lease',
  safety: 'Safety',
  density: 'Space',
  airQuality: 'Air quality',
};

const FACTOR_KEYS: (keyof Weights)[] = ['lease', 'safety', 'density', 'airQuality'];

export function partsOf(r: RankedFlat, weights: Weights, anchors: Anchor[]): Part[] {
  const legs = r.commute.length || 1;
  const out: Part[] = r.commute.map((leg) => {
    const key: PartKey = leg.anchorIndex === 0 ? 'w0' : 'w1';
    const share = 50 / legs;
    const a = anchors[leg.anchorIndex];
    return {
      key,
      kind: 'commute',
      label: a?.label ?? `Workplace ${leg.anchorIndex + 1}`,
      share,
      score: leg.score,
      points: (share * leg.score) / 100,
      raw: `${leg.minutes} min`,
      minutes: leg.minutes,
    };
  });
  for (const k of FACTOR_KEYS) {
    const share = (50 * weights[k]) / 100;
    const score = r.livability[k];
    out.push({
      key: k,
      kind: 'livability',
      label: FACTOR_SHORT[k],
      share,
      score,
      points: (share * score) / 100,
      raw: k === 'lease' ? `${r.flat.remainingLeaseYears} yrs left` : `score ${Math.round(score)}`,
      years: k === 'lease' ? r.flat.remainingLeaseYears : undefined,
    });
  }
  return out;
}

/** Point budget for the ranking formula itself (used on the search screen before there are results). */
export function budgetOf(weights: Weights, workplaces: string[]): { key: PartKey; label: string; share: number }[] {
  const legs = Math.max(1, workplaces.length);
  return [
    ...workplaces.slice(0, 2).map((w, i) => ({ key: (i === 0 ? 'w0' : 'w1') as PartKey, label: w || `Workplace ${i + 1}`, share: 50 / legs })),
    ...FACTOR_KEYS.map((k) => ({ key: k as PartKey, label: FACTOR_SHORT[k], share: (50 * weights[k]) / 100 })),
  ];
}

export type Averages = Partial<Record<PartKey, number>>;

/** Average score of each part across the result set: the "typical flat" a driver is measured against. */
export function averagesOf(list: RankedFlat[]): Averages {
  const sum: Record<string, number> = {};
  const n: Record<string, number> = {};
  for (const r of list) {
    for (const leg of r.commute) {
      const k = leg.anchorIndex === 0 ? 'w0' : 'w1';
      sum[k] = (sum[k] ?? 0) + leg.score;
      n[k] = (n[k] ?? 0) + 1;
    }
    for (const k of FACTOR_KEYS) {
      sum[k] = (sum[k] ?? 0) + r.livability[k];
      n[k] = (n[k] ?? 0) + 1;
    }
  }
  const out: Averages = {};
  for (const k of Object.keys(sum)) out[k as PartKey] = sum[k] / n[k];
  return out;
}

export interface Driver {
  part: Part;
  /** Points gained (+) or lost (-) against the typical flat in these results. */
  delta: number;
  text: string;
}

function driverText(p: Part, good: boolean): string {
  switch (p.key) {
    case 'w0':
    case 'w1':
      return good ? `Quick trip to ${p.label} (${p.raw})` : `Long trip to ${p.label} (${p.raw})`;
    case 'lease':
      return good ? `Long lease (${p.raw})` : `Shorter lease (${p.raw})`;
    case 'safety':
      return good ? `Safer town (${Math.round(p.score)}/100)` : `Less safe town (${Math.round(p.score)}/100)`;
    case 'density':
      return good ? `Roomier estate (${Math.round(p.score)}/100)` : `Crowded estate (${Math.round(p.score)}/100)`;
    case 'airQuality':
      return good ? `Cleaner air (${Math.round(p.score)}/100)` : `Poorer air (${Math.round(p.score)}/100)`;
  }
}

/** The part that helps most and the one that hurts most, relative to the average flat. */
export function driversOf(parts: Part[], avg: Averages): { helps: Driver | null; hurts: Driver | null; all: Driver[] } {
  const all = parts
    .filter((p) => p.share > 0)
    .map((p) => {
      const delta = (p.share * (p.score - (avg[p.key] ?? p.score))) / 100;
      return { part: p, delta, text: driverText(p, delta >= 0) };
    })
    .sort((a, b) => b.delta - a.delta);
  const helps = all[0] && all[0].delta > 0.15 ? all[0] : null;
  const last = all[all.length - 1];
  const hurts = last && last.delta < -0.15 ? last : null;
  return { helps, hurts, all };
}

function pairText(a: Part, b: Part, better: boolean): string {
  switch (a.key) {
    case 'w0':
    case 'w1':
      return `${better ? 'quicker' : 'longer'} trip to ${a.label} (${a.minutes} vs ${b.minutes} min)`;
    case 'lease':
      return `${better ? 'more' : 'less'} lease left (${a.years} vs ${b.years} yrs)`;
    case 'safety':
      return `${better ? 'safer' : 'less safe'} town (${Math.round(a.score)} vs ${Math.round(b.score)})`;
    case 'density':
      return `${better ? 'more space' : 'more crowded'} (${Math.round(a.score)} vs ${Math.round(b.score)})`;
    case 'airQuality':
      return `${better ? 'cleaner' : 'poorer'} air (${Math.round(a.score)} vs ${Math.round(b.score)})`;
  }
}

/**
 * Why flat A sits where it does relative to flat B: the score gap and the single part that explains most of
 * it (in the direction of the gap).
 */
export function compareParts(a: Part[], b: Part[]): { gap: number; reason: string | null; tradeOff: string | null } {
  const gap = a.reduce((s, p) => s + p.points, 0) - b.reduce((s, p) => s + p.points, 0);
  const diffs = a
    .map((pa) => {
      const pb = b.find((x) => x.key === pa.key);
      return pb ? { pa, pb, d: pa.points - pb.points } : null;
    })
    .filter((x): x is { pa: Part; pb: Part; d: number } => x !== null && Math.abs(x.d) > 0.05)
    .sort((x, y) => (gap >= 0 ? y.d - x.d : x.d - y.d));
  const main = diffs[0];
  const counter = diffs[diffs.length - 1];
  return {
    gap,
    reason: main && Math.sign(main.d) === Math.sign(gap || main.d) ? pairText(main.pa, main.pb, main.d > 0) : null,
    tradeOff: counter && counter !== main && Math.sign(counter.d) !== Math.sign(gap) ? pairText(counter.pa, counter.pb, counter.d > 0) : null,
  };
}

/** Why a flat moved after the livability weights changed. */
export function moveReason(r: RankedFlat, move: number, oldW: Weights, newW: Weights, avg: Averages): string | null {
  if (!move) return null;
  let best: { k: keyof Weights; c: number } | null = null;
  for (const k of FACTOR_KEYS) {
    const c = (0.5 * (newW[k] - oldW[k]) * (r.livability[k] - (avg[k] ?? r.livability[k]))) / 100;
    if (Math.sign(c) !== Math.sign(move)) continue;
    if (!best || Math.abs(c) > Math.abs(best.c)) best = { k, c };
  }
  if (!best) return null;
  const more = newW[best.k] > oldW[best.k];
  const strong = r.livability[best.k] >= (avg[best.k] ?? 0);
  return `${FACTOR_SHORT[best.k]} now counts ${more ? 'more' : 'less'}, and it is ${strong ? 'a strength' : 'a weak spot'} here (${Math.round(r.livability[best.k])}/100)`;
}

export const pts = (n: number) => (Math.abs(n) < 0.05 ? '0' : Math.abs(n) >= 10 ? Math.abs(n).toFixed(0) : Math.abs(n).toFixed(1));
