import { budgetOf, PART_COLOR } from '../lib/explain';
import type { Weights } from '../types/models';

/**
 * The ranking formula drawn as a 100-point budget: commute on the left half (split between workplaces),
 * livability on the right half (split by the user's weights). The same colours appear on every card.
 */
export function RankingFormula({ weights, workplaces, dense }: { weights: Weights; workplaces: string[]; dense?: boolean }) {
  const parts = budgetOf(weights, workplaces).filter((p) => p.share > 0);
  const fmt = (n: number) => (Number.isInteger(n) ? String(n) : n.toFixed(1));
  return (
    <div>
      <div className="flex text-[11px] font-semibold uppercase tracking-wide text-ink-500">
        <span className="w-1/2">Commute · 50 pts</span>
        <span className="w-1/2 pl-1">Livability · 50 pts</span>
      </div>
      <div className="mt-1 flex gap-[2px]" aria-hidden>
        {parts.map((p) => (
          <div
            key={p.key}
            className="flex h-6 min-w-0 items-center justify-center overflow-hidden rounded-[4px] text-[10px] font-bold text-white transition-[flex-grow] duration-500"
            style={{ flexGrow: p.share, flexBasis: 0, background: PART_COLOR[p.key] }}
            title={`${p.label}: up to ${fmt(p.share)} points`}
          >
            {p.share >= 7 ? fmt(p.share) : ''}
          </div>
        ))}
      </div>
      <ul className={`mt-1.5 flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-600 ${dense ? '' : 'sm:text-[13px]'}`}>
        {parts.map((p) => (
          <li key={p.key} className="flex min-w-0 items-center gap-1.5">
            <span className="h-2.5 w-2.5 shrink-0 rounded-sm" style={{ background: PART_COLOR[p.key] }} aria-hidden />
            <span className="max-w-[150px] truncate">{p.key === 'w0' || p.key === 'w1' ? `To ${p.label}` : p.label}</span>
            <b className="tabular-nums text-ink-800">{fmt(p.share)}</b>
          </li>
        ))}
      </ul>
    </div>
  );
}
