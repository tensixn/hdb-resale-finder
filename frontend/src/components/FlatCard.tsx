import { Bike, Bus, Car, Footprints, Star } from './icons';
import type { Anchor, RankedFlat, TransportMode } from '../types/models';
import { flatTypeLabel, moneyShort, titleCase } from '../lib/format';
import { ScoreRing } from './ScoreRing';
import { ValuationBadge } from './ValuationBadge';

export const MODE_ICON: Record<TransportMode, typeof Bus> = {
  PUBLIC_TRANSPORT: Bus,
  DRIVE: Car,
  CYCLE: Bike,
  WALK: Footprints,
};

/** One ranked result (FR-23): location, type, price, Livability Index, valuation label, commute. */
export function FlatCard({
  item,
  anchors,
  bookmarked,
  highlighted,
  onOpen,
  onHover,
  onBookmark,
}: {
  item: RankedFlat;
  anchors: Anchor[];
  bookmarked: boolean;
  highlighted: boolean;
  onOpen: () => void;
  onHover: (on: boolean) => void;
  onBookmark: () => void;
}) {
  const f = item.flat;
  return (
    <article
      id={`flat-${f.id}`}
      onMouseEnter={() => onHover(true)}
      onMouseLeave={() => onHover(false)}
      className={`group relative cursor-pointer rounded-2xl bg-white p-4 ring-1 transition duration-200 motion-safe:hover:-translate-y-0.5 ${
        highlighted ? 'shadow-pop ring-brand-400' : 'shadow-card ring-ink-900/5 hover:ring-ink-900/15'
      }`}
      onClick={onOpen}
    >
      <div className="flex gap-4">
        <div className="flex flex-col items-center gap-1">
          <ScoreRing value={item.matchScore} size={52} label="Match score" />
          <span className="text-[10px] font-semibold uppercase tracking-wide text-ink-400">Match</span>
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-wide text-ink-500">
                <span>#{item.rank}</span>
                <span className="h-1 w-1 rounded-full bg-ink-300" />
                <span className="truncate">{titleCase(f.town)}</span>
              </div>
              <h3 className="mt-0.5 truncate text-[15px] font-semibold text-ink-900">
                {f.block} {titleCase(f.street)}
              </h3>
              <p className="text-sm text-ink-500">
                {flatTypeLabel(f.flatType)} · {Math.round(f.floorAreaSqm)} m² · {f.remainingLeaseYears} yrs left
              </p>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onBookmark();
              }}
              aria-label={bookmarked ? 'Remove bookmark' : 'Bookmark this flat'}
              aria-pressed={bookmarked}
              className={`-mr-1 -mt-1 rounded-lg p-1.5 transition ${bookmarked ? 'text-amber-500' : 'text-ink-300 hover:bg-ink-50 hover:text-ink-500'}`}
            >
              <Star className="h-5 w-5" weight={bookmarked ? 'fill' : 'regular'} />
            </button>
          </div>

          <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-2">
            <span className="font-display text-xl font-semibold tracking-tight text-ink-900">{moneyShort(f.price)}</span>
            <ValuationBadge label={item.valuation} size="sm" />
            <span className="ml-auto inline-flex items-center gap-1 rounded-full bg-ink-50 px-2 py-0.5 text-xs font-medium text-ink-700" title="Livability Index">
              Livability <b className="tabular-nums text-ink-900">{Math.round(item.livability.index)}</b>
            </span>
          </div>

          <div className="mt-3 flex flex-wrap gap-2">
            {item.commute.map((leg) => {
              const Icon = MODE_ICON[leg.mode];
              const a = anchors[leg.anchorIndex];
              return (
                <span
                  key={leg.anchorIndex}
                  title={`${a?.label ?? 'Workplace'}: ${leg.minutes} min`}
                  className="inline-flex max-w-full items-center gap-1.5 rounded-lg bg-ink-50 px-2 py-1 text-xs text-ink-700"
                >
                  <span className={`flex h-4 w-4 items-center justify-center rounded text-[9px] font-bold text-white ${leg.anchorIndex === 0 ? 'bg-ink-900' : 'bg-[#a8472f]'}`}>
                    {leg.anchorIndex + 1}
                  </span>
                  <Icon className="h-3.5 w-3.5 text-ink-500" />
                  <b className="tabular-nums">{leg.minutes} min</b>
                  <span className="truncate text-ink-500">to {a?.label}</span>
                </span>
              );
            })}
          </div>
        </div>
      </div>
    </article>
  );
}
