import { scoreColor } from '../lib/format';

/** A circular 0-100 score, coloured on the same scale as the map pins. */
export function ScoreRing({ value, size = 48, label }: { value: number; size?: number; label?: string }) {
  const stroke = size >= 64 ? 6 : 4.5;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const pct = Math.max(0, Math.min(100, value)) / 100;
  return (
    <div className="relative inline-flex shrink-0 items-center justify-center" style={{ width: size, height: size }} aria-label={`${label ?? 'Score'} ${Math.round(value)} out of 100`}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="#eceef2" strokeWidth={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={scoreColor(value)}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={`${c * pct} ${c}`}
          className="transition-[stroke-dasharray] duration-500"
        />
      </svg>
      <span className={`absolute font-bold tabular-nums text-ink-900 ${size >= 64 ? 'text-xl' : 'text-sm'}`}>{Math.round(value)}</span>
    </div>
  );
}

/** A thin labelled bar for one 0-100 factor score. */
export function ScoreBar({ value, label, detail }: { value: number; label: string; detail?: string }) {
  return (
    <div>
      <div className="mb-1 flex items-baseline justify-between text-sm">
        <span className="text-ink-700">{label}</span>
        <span className="tabular-nums font-semibold text-ink-900">{Math.round(value)}</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-ink-100">
        <div className="h-full rounded-full transition-all duration-500" style={{ width: `${value}%`, background: scoreColor(value) }} />
      </div>
      {detail && <p className="mt-1 text-xs text-ink-500">{detail}</p>}
    </div>
  );
}
