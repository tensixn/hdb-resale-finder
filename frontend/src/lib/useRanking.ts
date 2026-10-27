import { useMemo } from 'react';
import type { ExplainedRow } from '../components/RankCard';
import { useSearch, type ExtraFilters, type RankBaseline } from '../state/searchStore';
import type { RankedFlat, SearchResult } from '../types/models';
import { averagesOf, compareParts, driversOf, moveReason, partsOf, pts, type Averages } from './explain';

export function passesExtra(r: RankedFlat, extra: ExtraFilters): boolean {
  if (extra.minLease != null && r.flat.remainingLeaseYears < extra.minLease) return false;
  if (extra.minArea != null && r.flat.floorAreaSqm < extra.minArea) return false;
  return true;
}

export interface Ranking {
  rows: ExplainedRow[];
  byId: Map<number, ExplainedRow>;
  avg: Averages;
  /** Flats the server returned before the client-side lease/area filters. */
  serverCount: number;
}

export function buildRanking(result: SearchResult | null, extra: ExtraFilters, baseline: RankBaseline | null): Ranking {
  if (!result) return { rows: [], byId: new Map(), avg: {}, serverCount: 0 };
  const list = result.results.filter((r) => passesExtra(r, extra)).sort((a, b) => a.rank - b.rank);
  const avg = averagesOf(list);
  const parts = list.map((r) => partsOf(r, result.weights, result.anchors));

  const rows: ExplainedRow[] = list.map((item, i) => {
    const rank = i + 1;
    const d = driversOf(parts[i], avg);
    let vs: ExplainedRow['vs'] = null;
    const otherIdx = i === 0 ? 1 : i - 1;
    if (list[otherIdx]) {
      const c = compareParts(parts[i], parts[otherIdx]);
      const other = otherIdx + 1;
      if (Math.abs(c.gap) < 0.05) {
        vs = { text: `Level with #${other}: the scores differ by less than 0.1 pt.${c.reason ? ` It has ${c.reason}${c.tradeOff ? ` but ${c.tradeOff}` : ''}.` : ''}`, other, ahead: i === 0 };
      } else if (i === 0) {
        vs = { text: `Leads #2 by ${pts(c.gap)} pts. Main reason: ${c.reason ?? 'small gains across the board'}.${c.tradeOff ? ` Gives up: ${c.tradeOff}.` : ''}`, other, ahead: true };
      } else {
        vs = { text: `${pts(c.gap)} pts behind #${other}. Main reason: ${c.reason ?? 'small losses across the board'}.${c.tradeOff ? ` Wins on: ${c.tradeOff}.` : ''}`, other, ahead: false };
      }
    }
    let move: number | null = null;
    let why: string | null = null;
    if (baseline) {
      const before = baseline.ranks[item.flat.id];
      move = before != null ? before - rank : null;
      if (move) why = moveReason(item, move, baseline.weights, result.weights, avg);
    }
    return { item, rank, parts: parts[i], helps: d.helps, hurts: d.hurts, vs, move, moveReason: why };
  });
  return { rows, byId: new Map(rows.map((r) => [r.item.flat.id, r])), avg, serverCount: result.results.length };
}

/** The current results with every rank explained, shared by the results and detail screens. */
export function useRanking(): Ranking {
  const result = useSearch((s) => s.result);
  const extra = useSearch((s) => s.extraFilters);
  const baseline = useSearch((s) => s.rankBaseline);
  return useMemo(() => buildRanking(result, extra, baseline), [result, extra, baseline]);
}
