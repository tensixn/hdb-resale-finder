import { money, moneyShort, monthLabel } from '../lib/format';

/**
 * Where the asking price sits against the fair-value range (FR-32). The middle band is the +/-5% fair zone.
 */
export function FairValueBar({ price, low, fair, high }: { price: number; low: number; fair: number; high: number }) {
  const span = high - low;
  const min = Math.min(price, low - span * 0.6);
  const max = Math.max(price, high + span * 0.6);
  const pos = (v: number) => `${((v - min) / (max - min)) * 100}%`;
  const priceColor = price < low ? '#059669' : price > high ? '#e11d48' : '#0284c7';
  return (
    <div className="pt-7">
      <div className="relative h-3 rounded-full bg-gradient-to-r from-emerald-100 via-sky-100 to-rose-100">
        <div className="absolute inset-y-0 rounded-full bg-sky-300/70 ring-1 ring-sky-400" style={{ left: pos(low), width: `calc(${pos(high)} - ${pos(low)})` }} />
        <div className="absolute -top-7 -translate-x-1/2 whitespace-nowrap rounded-md px-1.5 py-0.5 text-[11px] font-bold text-white" style={{ left: pos(price), background: priceColor }}>
          {moneyShort(price)}
        </div>
        <div className="absolute -top-1.5 h-6 w-1 -translate-x-1/2 rounded-full ring-2 ring-white" style={{ left: pos(price), background: priceColor }} />
        <div className="absolute top-0 h-3 w-px bg-sky-700/50" style={{ left: pos(fair) }} />
      </div>
      <div className="relative mt-2 h-4 text-[11px] text-ink-500">
        <span className="absolute -translate-x-1/2" style={{ left: pos(low) }}>
          {moneyShort(low)}
        </span>
        <span className="absolute -translate-x-1/2 font-semibold text-sky-800" style={{ left: pos(fair) }}>
          {moneyShort(fair)}
        </span>
        <span className="absolute -translate-x-1/2" style={{ left: pos(high) }}>
          {moneyShort(high)}
        </span>
      </div>
      <div className="sr-only">
        Price {money(price)}; fair range {money(low)} to {money(high)}.
      </div>
    </div>
  );
}

/** Small line chart of this flat's past resale prices. */
export function PriceHistory({ points }: { points: { month: string; price: number }[] }) {
  const pts = [...points].sort((a, b) => a.month.localeCompare(b.month));
  if (pts.length === 0) return null;
  if (pts.length === 1) {
    return (
      <p className="text-sm text-ink-600">
        One recorded sale: <b>{money(pts[0].price)}</b> in {monthLabel(pts[0].month)}.
      </p>
    );
  }
  const W = 320;
  const H = 110;
  const P = 18;
  const prices = pts.map((p) => p.price);
  const lo = Math.min(...prices) * 0.97;
  const hi = Math.max(...prices) * 1.03;
  const x = (i: number) => 6 + (i / (pts.length - 1)) * (W - 12);
  const y = (v: number) => H - P - ((v - lo) / (hi - lo)) * (H - 2 * P);
  const d = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i)},${y(p.price)}`).join(' ');
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img" aria-label="Past resale prices">
      <path d={`${d} L${x(pts.length - 1)},${H - P} L${x(0)},${H - P} Z`} fill="#1d7a6b" opacity={0.08} />
      <path d={d} fill="none" stroke="#1d7a6b" strokeWidth={2.5} strokeLinejoin="round" />
      {pts.map((p, i) => (
        <g key={p.month + i}>
          <circle cx={x(i)} cy={y(p.price)} r={4} fill="#fff" stroke="#1d7a6b" strokeWidth={2} />
          <text x={x(i)} y={y(p.price) - 9} textAnchor={i === 0 ? 'start' : i === pts.length - 1 ? 'end' : 'middle'} className="fill-ink-800 text-[10px] font-semibold">
            {moneyShort(p.price)}
          </text>
          <text x={x(i)} y={H - 3} textAnchor={i === 0 ? 'start' : i === pts.length - 1 ? 'end' : 'middle'} className="fill-ink-500 text-[10px]">
            {monthLabel(p.month)}
          </text>
        </g>
      ))}
    </svg>
  );
}
