import { moneyShort, titleCase } from '../lib/format';
import type { ExplainedRow } from './RankCard';

const avgMinutes = (r: ExplainedRow) =>
  r.item.commute.length ? r.item.commute.reduce((s, l) => s + l.minutes, 0) / r.item.commute.length : 0;

/** "The short version": the best flat overall and the best on commute, price and livability, each one tap away. */
export function ShortVersion({ rows, onPick }: { rows: ExplainedRow[]; onPick: (flatId: number) => void }) {
  const best = (fn: (a: ExplainedRow, b: ExplainedRow) => number) => [...rows].sort((a, b) => fn(a, b) || a.rank - b.rank)[0];
  const fast = best((a, b) => avgMinutes(a) - avgMinutes(b));
  const cheap = best((a, b) => a.item.flat.price - b.item.flat.price);
  const livable = best((a, b) => b.item.livability.index - a.item.livability.index);
  const picks = [
    { title: 'Best overall', row: rows[0], figure: `${Math.round(rows[0].item.matchScore)}`, unit: 'match' },
    { title: 'Shortest commute', row: fast, figure: `${Math.round(avgMinutes(fast))}`, unit: fast.item.commute.length > 1 ? 'min avg' : 'min' },
    { title: 'Lowest price', row: cheap, figure: moneyShort(cheap.item.flat.price), unit: '' },
    { title: 'Most livable', row: livable, figure: `${Math.round(livable.item.livability.index)}`, unit: 'livability' },
  ];
  return (
    <section aria-label="The short version" className="rounded-2xl bg-white p-3 shadow-card ring-1 ring-ink-900/5">
      <h2 className="px-1 text-[13px] font-semibold text-ink-800">The short version</h2>
      <div className="mt-2 grid grid-cols-2 gap-2">
        {picks.map((p) => (
          <button
            key={p.title}
            type="button"
            onClick={() => onPick(p.row.item.flat.id)}
            className="min-h-[44px] rounded-xl bg-ink-50 px-3 py-2 text-left ring-1 ring-inset ring-ink-900/5 transition hover:bg-brand-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
          >
            <div className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">{p.title}</div>
            <div className="mt-0.5 text-lg font-bold leading-tight tabular-nums text-ink-900">
              {p.figure} {p.unit && <span className="text-xs font-medium text-ink-500">{p.unit}</span>}
            </div>
            <div className="truncate text-xs text-ink-600">
              #{p.row.rank} · {p.row.item.flat.block} {titleCase(p.row.item.flat.street)}
            </div>
          </button>
        ))}
      </div>
    </section>
  );
}
