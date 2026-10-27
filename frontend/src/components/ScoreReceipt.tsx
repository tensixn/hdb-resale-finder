import { ArrowDown, ArrowUp } from './icons';
import type { Averages, Part } from '../lib/explain';
import { Swatch } from './ScoreStack';

/**
 * The match score as an itemised receipt: each part, what it measured, its score, how many points it can give
 * and how many it gave. Optional comparison against the typical flat in the results.
 */
export function ScoreReceipt({ parts, avg, compact }: { parts: Part[]; avg?: Averages; compact?: boolean }) {
  const total = parts.reduce((s, p) => s + p.points, 0);
  const rows = (kind: Part['kind']) => parts.filter((p) => p.kind === kind && p.share > 0);
  const sub = (kind: Part['kind']) => rows(kind).reduce((s, p) => s + p.points, 0);
  const pad = compact ? 'py-1' : 'py-1.5';

  const Row = ({ p }: { p: Part }) => {
    const a = avg?.[p.key];
    const d = a != null ? (p.share * (p.score - a)) / 100 : null;
    return (
      <tr className="border-t border-ink-100">
        <td className={`${pad} pr-2`}>
          <span className="flex min-w-0 items-center gap-2">
            <Swatch k={p.key} />
            <span className="truncate">{p.kind === 'commute' ? `To ${p.label}` : p.label}</span>
          </span>
        </td>
        <td className={`${pad} hidden pr-2 text-ink-500 sm:table-cell`}>{p.raw}</td>
        <td className={`${pad} pr-2 text-right tabular-nums`}>{Math.round(p.score)}</td>
        <td className={`${pad} pr-2 text-right tabular-nums text-ink-500`}>{p.share.toFixed(p.share % 1 ? 1 : 0)}</td>
        <td className={`${pad} text-right font-semibold tabular-nums`}>{p.points.toFixed(1)}</td>
        {avg && (
          <td className={`${pad} pl-2 text-right tabular-nums`}>
            {d != null && Math.abs(d) >= 0.1 ? (
              <span className={`inline-flex items-center justify-end gap-0.5 text-xs font-semibold ${d > 0 ? 'text-emerald-700' : 'text-rose-700'}`}>
                {d > 0 ? <ArrowUp className="h-3 w-3" aria-hidden /> : <ArrowDown className="h-3 w-3" aria-hidden />}
                {d > 0 ? '+' : '−'}
                {Math.abs(d).toFixed(1)}
              </span>
            ) : (
              <span className="text-xs text-ink-400">≈</span>
            )}
          </td>
        )}
      </tr>
    );
  };

  const Group = ({ kind, title }: { kind: Part['kind']; title: string }) => (
    <>
      <tr>
        <th colSpan={avg ? 6 : 5} className="pb-0.5 pt-2 text-left text-[11px] font-semibold uppercase tracking-wide text-ink-500">
          {title} <span className="font-medium normal-case tracking-normal text-ink-400">· {sub(kind).toFixed(1)} of 50 pts</span>
        </th>
      </tr>
      {rows(kind).map((p) => (
        <Row key={p.key} p={p} />
      ))}
    </>
  );

  return (
    <table className="w-full table-fixed text-[13px] text-ink-800">
      <thead>
        <tr className="text-[11px] text-ink-500">
          <th className="text-left font-medium">Part</th>
          <th className="hidden w-[84px] text-left font-medium sm:table-cell">Measured</th>
          <th className="w-[44px] text-right font-medium">Score</th>
          <th className="w-[40px] text-right font-medium" title="Most points this part can give">Max</th>
          <th className="w-[44px] text-right font-medium">Pts</th>
          {avg && (
            <th className="w-[58px] text-right font-medium" title="Points gained or lost against the average flat in your results">
              vs avg
            </th>
          )}
        </tr>
      </thead>
      <tbody>
        <Group kind="commute" title="Commute" />
        <Group kind="livability" title="Livability" />
        <tr className="border-t-2 border-ink-200">
          <td className="pt-2 font-semibold" colSpan={4}>
            Match score
          </td>
          <td className="pt-2 text-right text-base font-bold tabular-nums">{total.toFixed(1)}</td>
          {avg && <td />}
        </tr>
      </tbody>
    </table>
  );
}
