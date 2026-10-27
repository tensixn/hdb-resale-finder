import type { TransportMode, ValuationLabel } from '../types/models';

const money0 = new Intl.NumberFormat('en-SG', { style: 'currency', currency: 'SGD', maximumFractionDigits: 0 });

export const money = (v: number | null | undefined) => (v == null ? '–' : money0.format(v).replace('SGD', 'S$'));

/** S$612k / S$1.05M, for compact cards and map labels. */
export function moneyShort(v: number | null | undefined): string {
  if (v == null) return '–';
  if (v >= 1_000_000) return `S$${(v / 1_000_000).toFixed(2).replace(/\.?0+$/, '')}M`;
  return `S$${Math.round(v / 1000)}k`;
}

export function titleCase(s: string): string {
  return s
    .toLowerCase()
    .split(' ')
    .map((w) => (w ? w[0].toUpperCase() + w.slice(1) : w))
    .join(' ')
    .replace(/\/(\w)/g, (_, c: string) => '/' + c.toUpperCase());
}

export const flatTypeLabel = (t: string) => titleCase(t).replace(' Room', '-room');

export function monthLabel(ym: string | null | undefined): string {
  if (!ym) return '–';
  const [y, m] = ym.split('-').map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString('en-SG', { month: 'short', year: 'numeric' });
}

export function dateLabel(iso: string | null | undefined): string {
  if (!iso) return '–';
  return new Date(iso).toLocaleString('en-SG', { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

export const MODE_LABEL: Record<TransportMode, string> = {
  PUBLIC_TRANSPORT: 'Public transport',
  DRIVE: 'Drive',
  CYCLE: 'Cycle',
  WALK: 'Walk',
};

export const VALUATION: Record<ValuationLabel, { label: string; short: string; tone: string; dot: string; help: string }> = {
  UNDER_VALUED: {
    label: 'Under-valued',
    short: 'Under',
    tone: 'bg-emerald-50 text-emerald-800 ring-emerald-600/20',
    dot: 'bg-emerald-500',
    help: 'Priced more than 5% below the estate average for its size.',
  },
  FAIR_VALUED: {
    label: 'Fair-valued',
    short: 'Fair',
    tone: 'bg-sky-50 text-sky-800 ring-sky-600/20',
    dot: 'bg-sky-500',
    help: 'Within 5% of the estate average for its size.',
  },
  OVER_VALUED: {
    label: 'Over-valued',
    short: 'Over',
    tone: 'bg-rose-50 text-rose-800 ring-rose-600/20',
    dot: 'bg-rose-500',
    help: 'Priced more than 5% above the estate average for its size.',
  },
  INSUFFICIENT_DATA: {
    label: 'No estimate',
    short: 'n/a',
    tone: 'bg-ink-100 text-ink-600 ring-ink-500/20',
    dot: 'bg-ink-400',
    help: 'Not enough recent sales of this flat type in this town.',
  },
};

/** Colour for a 0-100 score: warm for low, green for high. Used for map pins and score rings (FR-17). */
export function scoreColor(score: number): string {
  const stops: [number, [number, number, number]][] = [
    [0, [225, 85, 60]],
    [45, [235, 160, 50]],
    [65, [200, 190, 60]],
    [80, [70, 170, 110]],
    [100, [20, 120, 100]],
  ];
  const s = Math.max(0, Math.min(100, score));
  for (let i = 1; i < stops.length; i++) {
    if (s <= stops[i][0]) {
      const [a, ca] = stops[i - 1];
      const [b, cb] = stops[i];
      const t = (s - a) / (b - a);
      const c = ca.map((v, k) => Math.round(v + (cb[k] - v) * t));
      return `rgb(${c[0]},${c[1]},${c[2]})`;
    }
  }
  return 'rgb(20,120,100)';
}

export const FACTORS = [
  { key: 'lease', factor: 'LEASE', label: 'Remaining lease', help: 'Years left on the 99-year lease.' },
  { key: 'safety', factor: 'SAFETY', label: 'Safety', help: 'Lower reported crime per 1,000 residents in the town scores higher.' },
  { key: 'density', factor: 'DENSITY', label: 'Space', help: 'Lower residential density (residents per hectare) scores higher.' },
  { key: 'airQuality', factor: 'AIR_QUALITY', label: 'Air quality', help: "Lower 24-hour PSI in the flat's region scores higher." },
] as const;
