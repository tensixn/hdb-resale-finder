import { ArrowDown, ArrowRight, ArrowUp, ChevronDown, Minus, Plus, Star, TriangleAlert } from './icons';
import { memo } from 'react';
import { Link } from 'react-router-dom';
import { pts, type Averages, type Driver, type Part } from '../lib/explain';
import { flatTypeLabel, moneyShort, titleCase, VALUATION } from '../lib/format';
import { fairGap, useValuation } from '../lib/useValuation';
import type { RankedFlat } from '../types/models';
import { ScoreReceipt } from './ScoreReceipt';
import { ScoreStack } from './ScoreStack';

export interface ExplainedRow {
  item: RankedFlat;
  rank: number;
  parts: Part[];
  helps: Driver | null;
  hurts: Driver | null;
  /** How this flat stands against its neighbour in the ranking. */
  vs: { text: string; other: number; ahead: boolean } | null;
  /** Places moved since the priorities last changed (+ up, - down), or null with no baseline. */
  move: number | null;
  moveReason: string | null;
}

export function MoveBadge({ move, size = 'sm' }: { move: number | null; size?: 'sm' | 'md' }) {
  if (move == null) return null;
  const cls = size === 'md' ? 'px-2 py-0.5 text-xs' : 'px-1.5 py-px text-[11px]';
  if (move === 0)
    return (
      <span className={`inline-flex items-center gap-0.5 rounded-full bg-ink-100 font-semibold text-ink-500 ${cls}`} title="Same place as before">
        <Minus className="h-3 w-3" aria-hidden />
        <span className="sr-only">No change</span>
        <span aria-hidden>0</span>
      </span>
    );
  const up = move > 0;
  return (
    <span className={`inline-flex items-center gap-0.5 rounded-full font-bold ${cls} ${up ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}`}>
      {up ? <ArrowUp className="h-3 w-3" aria-hidden /> : <ArrowDown className="h-3 w-3" aria-hidden />}
      <span className="sr-only">{up ? 'Up' : 'Down'}</span>
      {Math.abs(move)}
      <span className="sr-only"> places since priorities changed</span>
    </span>
  );
}

function RankCardImpl({
  row,
  avg,
  bookmarked,
  selected,
  hovered,
  onSelect,
  onHover,
  onOpen,
  onBookmark,
}: {
  row: ExplainedRow;
  avg: Averages;
  bookmarked: boolean;
  selected: boolean;
  hovered: boolean;
  onSelect: (id: number) => void;
  onHover: (id: number | null) => void;
  onOpen: (id: number) => void;
  onBookmark: (id: number) => void;
}) {
  const { item, rank, parts } = row;
  const f = item.flat;
  const val = useValuation(f.id);
  const gap = fairGap(val);
  const legs = item.commute;

  return (
    <article
      id={`flat-${f.id}`}
      tabIndex={0}
      aria-label={`Rank ${rank}: ${f.block} ${titleCase(f.street)}, match ${Math.round(item.matchScore)}`}
      aria-expanded={selected}
      onMouseEnter={() => onHover(f.id)}
      onMouseLeave={() => onHover(null)}
      onFocus={(e) => e.target === e.currentTarget && onHover(f.id)}
      onClick={() => onSelect(f.id)}
      onKeyDown={(e) => {
        if (e.target !== e.currentTarget) return;
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect(f.id);
        }
      }}
      className={`group relative cursor-pointer scroll-mt-3 rounded-2xl bg-white ring-1 transition duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 ${
        selected ? 'shadow-pop ring-2 ring-brand-600' : hovered ? 'shadow-pop ring-brand-300' : 'shadow-card ring-ink-900/5 hover:ring-ink-900/15'
      }`}
    >
      <div className="flex gap-3 p-3.5 pb-3 sm:gap-3.5 sm:p-4 sm:pb-3">
        {/* Rank column */}
        <div className="flex w-11 shrink-0 flex-col items-center pt-0.5">
          <span className={`text-[22px] font-extrabold leading-none tracking-tight tabular-nums ${rank <= 3 ? 'text-brand-700' : 'text-ink-800'}`}>
            <span className="text-[13px] font-bold text-ink-400">#</span>
            {rank}
          </span>
          <span className="mt-1.5">
            <MoveBadge move={row.move} />
          </span>
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-start gap-2">
            <div className="min-w-0 flex-1">
              <h3 className="truncate text-[15px] font-semibold leading-tight text-ink-900">
                <Link
                  to={`/flats/${f.id}`}
                  onClick={(e) => e.stopPropagation()}
                  className="rounded hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
                >
                  {f.block} {titleCase(f.street)}
                </Link>
              </h3>
              <p className="flat-meta mt-0.5 text-[13px] leading-snug text-ink-500">
                {titleCase(f.town)} · {flatTypeLabel(f.flatType)} · {Math.round(f.floorAreaSqm)} m² · {f.remainingLeaseYears} yrs left
              </p>
            </div>
            <div className="shrink-0 text-right">
              <div className="text-[22px] font-bold leading-none tabular-nums text-ink-900">{Math.round(item.matchScore)}</div>
              <div className="text-[10px] font-semibold uppercase tracking-wide text-ink-400">match</div>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onBookmark(f.id);
              }}
              aria-label={bookmarked ? 'Remove bookmark' : 'Bookmark this flat'}
              aria-pressed={bookmarked}
              className={`-mr-2 -mt-2 flex h-11 w-11 shrink-0 items-center justify-center rounded-xl transition focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 ${
                bookmarked ? 'text-amber-500' : 'text-ink-300 hover:bg-ink-50 hover:text-ink-500'
              }`}
            >
              <Star className="h-5 w-5" weight={bookmarked ? 'fill' : 'regular'} />
            </button>
          </div>

          {/* Score decomposition */}
          <div className="mt-2.5">
            <div className="mb-1 flex text-[11px] text-ink-500">
              <span className="flex w-1/2 min-w-0 items-center gap-1.5 pr-1">
                <span className="font-medium">
                  Commute <b className="tabular-nums text-ink-800">{Math.round(item.commuteScore)}</b>
                </span>
                <span className="truncate tabular-nums text-ink-400">
                  {legs.map((l) => `${l.minutes}m`).join(' · ')}
                </span>
              </span>
              <span className="w-1/2 pl-1 font-medium">
                Livability <b className="tabular-nums text-ink-800">{Math.round(item.livability.index)}</b>
              </span>
            </div>
            <ScoreStack parts={parts} />
          </div>

          {/* Drivers */}
          <ul className="mt-2.5 space-y-1 text-[13px] leading-snug">
            {row.helps && (
              <li className="flex items-start gap-1.5 text-ink-800">
                <span className="mt-px flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-700" aria-hidden>
                  <Plus className="h-3 w-3" weight="bold" />
                </span>
                <span className="min-w-0">
                  <span className="sr-only">Helps most: </span>
                  {row.helps.text} <span className="whitespace-nowrap text-xs font-semibold text-emerald-700">+{pts(row.helps.delta)} pts</span>
                </span>
              </li>
            )}
            {row.hurts && (
              <li className="flex items-start gap-1.5 text-ink-800">
                <span className="mt-px flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-rose-100 text-rose-700" aria-hidden>
                  <Minus className="h-3 w-3" weight="bold" />
                </span>
                <span className="min-w-0">
                  <span className="sr-only">Costs most: </span>
                  {row.hurts.text} <span className="whitespace-nowrap text-xs font-semibold text-rose-700">−{pts(row.hurts.delta)} pts</span>
                </span>
              </li>
            )}
          </ul>

          {row.move != null && row.move !== 0 && row.moveReason && (
            <p className={`mt-2 rounded-lg px-2.5 py-1.5 text-xs leading-snug ${row.move > 0 ? 'bg-emerald-50 text-emerald-900' : 'bg-rose-50 text-rose-900'}`}>
              <b>{row.move > 0 ? `Up ${row.move}` : `Down ${-row.move}`}:</b> {row.moveReason}.
            </p>
          )}

          {row.vs && (
            <p className="mt-2 border-l-2 border-ink-200 pl-2 text-xs leading-snug text-ink-600">
              {row.vs.text}
            </p>
          )}

          {/* Price, kept apart from the score on purpose */}
          <div className="mt-2.5 flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="text-base font-bold tracking-tight text-ink-900">{moneyShort(f.price)}</span>
            <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset ${VALUATION[item.valuation].tone}`} title={VALUATION[item.valuation].help}>
              <span className={`h-1.5 w-1.5 rounded-full ${VALUATION[item.valuation].dot}`} aria-hidden />
              {VALUATION[item.valuation].label}
            </span>
            {gap && (
              <span className="text-xs text-ink-500">
                {Math.abs(gap.pct) < 0.5 ? 'at' : `${Math.abs(gap.pct).toFixed(0)}% ${gap.pct > 0 ? 'above' : 'below'}`} fair {moneyShort(val!.fairValue)}
              </span>
            )}
            {item.commuteOutdated && (
              <span className="inline-flex items-center gap-1 text-[11px] font-medium text-amber-800">
                <TriangleAlert className="h-3 w-3" aria-hidden /> Commute may be outdated
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="flex items-stretch border-t border-ink-100">
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onSelect(f.id);
          }}
          aria-expanded={selected}
          className="flex min-h-[44px] flex-1 items-center gap-1.5 rounded-bl-2xl px-4 text-left text-[13px] font-medium text-ink-600 hover:bg-ink-50 hover:text-ink-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-600 sm:min-h-[38px]"
        >
          <ChevronDown className={`h-4 w-4 transition ${selected ? 'rotate-180' : ''}`} aria-hidden />
          {selected ? 'Hide score breakdown' : `Why #${rank}? Score breakdown`}
        </button>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onOpen(f.id);
          }}
          className="flex min-h-[44px] items-center gap-1 rounded-br-2xl border-l border-ink-100 px-4 text-[13px] font-semibold text-brand-700 hover:bg-brand-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-600 sm:min-h-[38px]"
        >
          Details <ArrowRight className="h-3.5 w-3.5" aria-hidden />
        </button>
      </div>

      {selected && (
        <div className="cursor-default border-t border-ink-100 bg-ink-50/70 px-4 pb-4 pt-2" onClick={(e) => e.stopPropagation()}>
          <ScoreReceipt parts={parts} avg={avg} compact />
          <p className="mt-3 text-[11px] leading-relaxed text-ink-500">
            “vs avg” is points gained or lost against the average flat in these results. Price and fair value are not part of the score; they are shown so you can weigh value yourself.
          </p>
        </div>
      )}
    </article>
  );
}

export const RankCard = memo(RankCardImpl);
