import { ArrowLeft, ArrowRight, ChevronDown, ChevronLeft, ChevronRight, List, Map as MapIcon, RotateCcw, SearchX, SlidersHorizontal, Sparkles, X } from '../components/icons';
import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/apiClient';
import { FilterPanel } from '../components/FilterPanel';
import { LivabilitySliders } from '../components/LivabilitySliders';
import { MapView } from '../components/MapView';
import { PriorityPresets } from '../components/PriorityPresets';
import { ShortVersion } from '../components/ShortVersion';
import { MoveBadge, RankCard, type ExplainedRow } from '../components/RankCard';
import { RankingFormula } from '../components/RankingFormula';
import { ScoreStack } from '../components/ScoreStack';
import { WarningBanner } from '../components/WarningBanner';
import { Button, EmptyState, Spinner } from '../components/ui';
import { FACTOR_SHORT } from '../lib/explain';
import { flatTypeLabel, MODE_LABEL, money, moneyShort, titleCase } from '../lib/format';
import { activePreset } from '../lib/priorities';
import { useBookmarkAction } from '../lib/useBookmarkAction';
import { useRanking } from '../lib/useRanking';
import { useBookmarks } from '../state/bookmarkStore';
import { useSearch } from '../state/searchStore';
import type { Weights } from '../types/models';

type SortKey = 'match' | 'price' | 'commute' | 'livability';
const SORTS: { key: SortKey; label: string; fn: (a: ExplainedRow, b: ExplainedRow) => number }[] = [
  { key: 'match', label: 'Best match', fn: (a, b) => a.rank - b.rank },
  { key: 'price', label: 'Lowest price', fn: (a, b) => a.item.flat.price - b.item.flat.price },
  { key: 'commute', label: 'Best commute', fn: (a, b) => b.item.commuteScore - a.item.commuteScore },
  { key: 'livability', label: 'Most livable', fn: (a, b) => b.item.livability.index - a.item.livability.index },
];
const PAGE = 40;
const W_KEYS: (keyof Weights)[] = ['lease', 'safety', 'density', 'airQuality'];
const sameWeights = (a: Weights, b: Weights) => W_KEYS.every((k) => Math.abs(a[k] - b[k]) < 0.01);

/** Results (map + ranked list): UC-01 output, UC-03 re-weighting and UC-10 filtering, with every rank explained. */
export default function ResultsMapListScreen() {
  const navigate = useNavigate();
  const s = useSearch();
  const ranking = useRanking();
  const bookmarkIds = useBookmarks((b) => b.ids);
  const onBookmark = useBookmarkAction();
  const [view, setView] = useState<'list' | 'map'>('list');
  const [panel, setPanel] = useState<'none' | 'filters' | 'weights'>('none');
  const [sort, setSort] = useState<SortKey>('match');
  const [limit, setLimit] = useState(PAGE);
  const [towns, setTowns] = useState<string[]>([]);
  const listRef = useRef<HTMLDivElement>(null);
  const [phone] = useState(() => window.matchMedia('(max-width: 767px)').matches);

  useEffect(() => {
    s.restore();
    api.options().then((o) => setTowns(o.towns)).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtersKey = JSON.stringify([s.filters, s.extraFilters]);
  useEffect(() => setLimit(PAGE), [s.result?.searchId, filtersKey, sort]);

  // Remember the order before a change of priorities so each card can say how far it moved and why.
  const last = useRef<{ searchId: string; filtersKey: string; weights: Weights; ranks: Record<number, number>; topId: number | null } | null>(null);
  useEffect(() => {
    const r = s.result;
    if (!r) return;
    const cur = {
      searchId: r.searchId,
      filtersKey,
      weights: r.weights,
      ranks: Object.fromEntries(ranking.rows.map((x) => [x.item.flat.id, x.rank])),
      topId: ranking.rows[0]?.item.flat.id ?? null,
    };
    const prev = last.current;
    if (prev) {
      if (prev.searchId === cur.searchId && prev.filtersKey === cur.filtersKey && !sameWeights(prev.weights, cur.weights)) {
        s.setRankBaseline({ weights: prev.weights, ranks: prev.ranks, topId: prev.topId });
        listRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
      } else if (prev.searchId !== cur.searchId || prev.filtersKey !== cur.filtersKey) {
        s.setRankBaseline(null);
      }
    }
    last.current = cur;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [s.result, filtersKey]);

  const sorted = useMemo(() => [...ranking.rows].sort(SORTS.find((x) => x.key === sort)!.fn), [ranking.rows, sort]);

  // Keep the list in step with the map: a flat picked on the map scrolls into view.
  useEffect(() => {
    if (s.selectedFlatId == null) return;
    const idx = sorted.findIndex((r) => r.item.flat.id === s.selectedFlatId);
    if (idx >= limit) setLimit(idx + 10);
    requestAnimationFrame(() => document.getElementById(`flat-${s.selectedFlatId}`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }));
  }, [s.selectedFlatId, sorted, limit]);

  const open = useCallback((id: number) => navigate(`/flats/${id}`), [navigate]);
  const select = useCallback((id: number | null) => {
    const cur = useSearch.getState().selectedFlatId;
    useSearch.getState().setSelected(id != null && id === cur ? null : id);
  }, []);
  const selectFromMap = useCallback((id: number | null) => useSearch.getState().setSelected(id), []);
  const hover = useCallback((id: number | null) => useSearch.getState().setHovered(id), []);
  const bookmark = useCallback((id: number) => onBookmark(id), [onBookmark]);

  if (!s.searchId && !s.result) {
    return (
      <EmptyState icon={<SearchX className="h-6 w-6" />} title="No search yet">
        <p>Start with your workplace and what matters to you.</p>
        <Button className="mt-4" onClick={() => navigate('/')}>
          Start a search
        </Button>
      </EmptyState>
    );
  }
  if (!s.result) return <Spinner label="Loading your results" />;

  const r = s.result;
  const ex = s.extraFilters;
  const chips: { label: string; clear: () => void }[] = [
    s.filters.minPrice ? { label: `From ${money(s.filters.minPrice)}`, clear: () => s.setFilters({ ...s.filters, minPrice: null }) } : null,
    s.filters.maxPrice ? { label: `Up to ${money(s.filters.maxPrice)}`, clear: () => s.setFilters({ ...s.filters, maxPrice: null }) } : null,
    ...s.filters.flatTypes.map((t) => ({ label: flatTypeLabel(t), clear: () => s.setFilters({ ...s.filters, flatTypes: s.filters.flatTypes.filter((x) => x !== t) }) })),
    ...s.filters.towns.map((t) => ({ label: titleCase(t), clear: () => s.setFilters({ ...s.filters, towns: s.filters.towns.filter((x) => x !== t) }) })),
    ex.minLease != null ? { label: `${ex.minLease}+ yrs lease`, clear: () => s.setExtraFilters({ ...ex, minLease: null }) } : null,
    ex.minArea != null ? { label: `${ex.minArea}+ m²`, clear: () => s.setExtraFilters({ ...ex, minArea: null }) } : null,
  ].filter((x): x is { label: string; clear: () => void } => x !== null);

  const clearAll = () => {
    s.setExtraFilters({ minLease: null, minArea: null });
    s.setFilters({ minPrice: null, maxPrice: null, flatTypes: [], towns: [] });
  };
  const modes = Array.from(new Set(r.results[0]?.commute.map((c) => c.mode) ?? []));
  const shown = ranking.rows.length;
  const baseline = s.rankBaseline;

  const priorityChanges = baseline
    ? W_KEYS.filter((k) => Math.round(baseline.weights[k]) !== Math.round(r.weights[k])).map(
        (k) => `${FACTOR_SHORT[k]} ${Math.round(baseline.weights[k])}→${Math.round(r.weights[k])}%`,
      )
    : [];
  const movedUp = ranking.rows.filter((x) => (x.move ?? 0) > 0).length;
  const movedDown = ranking.rows.filter((x) => (x.move ?? 0) < 0).length;
  const newTop = ranking.rows[0];
  const climber = [...ranking.rows].sort((a, b) => (b.move ?? 0) - (a.move ?? 0))[0];

  const shortVersion = sort === 'match' && ranking.rows.length > 3 && <ShortVersion rows={ranking.rows} onPick={(id) => s.setSelected(id)} />;

  const pillBtn = (active: boolean) =>
    `inline-flex min-h-[44px] items-center gap-1.5 rounded-xl px-3 text-sm font-medium ring-1 ring-inset transition focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 sm:min-h-[36px] ${
      active ? 'bg-brand-50 text-brand-800 ring-brand-300' : 'bg-white text-ink-700 ring-ink-200 hover:bg-ink-50'
    }`;

  const listPane = (
    <div ref={listRef} className="h-full overflow-y-auto overscroll-contain bg-ink-50">
      <div className="bg-white px-4 pb-3 pt-3">
        <button
          onClick={() => navigate('/')}
          className="-ml-1 inline-flex min-h-[32px] items-center gap-1 rounded-lg px-1 text-xs font-medium text-ink-500 hover:text-ink-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 max-sm:min-h-[44px]"
        >
          <ArrowLeft className="h-3.5 w-3.5" /> Edit search
        </button>
        <h1 className="text-lg font-bold leading-tight tracking-tight">
          {shown} flat{shown === 1 ? '' : 's'} ranked for you
          {shown !== r.totalMatched && <span className="text-sm font-medium text-ink-500"> of {r.totalMatched} matched</span>}
        </h1>
        <p className="mt-0.5 truncate text-[13px] text-ink-500">
          {r.anchors.map((a, i) => (
            <span key={a.index}>
              {i > 0 && ' + '}
              <b className="font-medium text-ink-700">{a.label}</b>
            </span>
          ))}
          {modes.length > 0 && <> · {modes.map((m) => MODE_LABEL[m]).join(' / ')}</>}
        </p>

        {/* The ranking formula */}
        <details open={!phone} className="group mt-3 rounded-xl bg-ink-50 px-3 pb-2.5 pt-2 ring-1 ring-inset ring-ink-900/5">
          <summary className="flex min-h-[28px] cursor-pointer list-none items-center justify-between gap-2 rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 max-sm:min-h-[44px] [&::-webkit-details-marker]:hidden">
            <span className="text-[13px] font-semibold text-ink-800">How the order is decided</span>
            <span className="inline-flex items-center gap-1 text-[11px] text-ink-500">
              Match = points out of 100
              <ChevronDown className="h-3.5 w-3.5 transition group-open:rotate-180" aria-hidden />
            </span>
          </summary>
          <div className="mt-1.5">
            <RankingFormula weights={r.weights} workplaces={r.anchors.map((a) => a.label)} dense />
          </div>
        </details>
      </div>

      {/* Toolbar stays in reach while scrolling, with its panel */}
      <div className="sticky top-0 z-10 border-y border-ink-900/5 bg-white/95 backdrop-blur">
        <div className="flex flex-wrap items-center gap-2 px-4 py-2">
          <button onClick={() => setPanel(panel === 'weights' ? 'none' : 'weights')} className={pillBtn(panel === 'weights')} aria-expanded={panel === 'weights'}>
            <Sparkles className="h-4 w-4" /> Priorities
            <span className="font-normal text-ink-500 max-sm:hidden">· {activePreset(s.weights)?.label ?? 'Custom'}</span>
          </button>
          <button onClick={() => setPanel(panel === 'filters' ? 'none' : 'filters')} className={pillBtn(panel === 'filters')} aria-expanded={panel === 'filters'}>
            <SlidersHorizontal className="h-4 w-4" /> Filters
            {chips.length > 0 && <span className="rounded-full bg-brand-600 px-1.5 text-[11px] text-white">{chips.length}</span>}
          </button>
          {s.refreshing ? (
            <span className="text-xs text-ink-500" role="status">
              Re-ranking…
            </span>
          ) : (
            baseline &&
            priorityChanges.length > 0 && (
              <span className="text-xs font-semibold" title="Since you last changed priorities">
                <span className="text-emerald-700">▲{movedUp}</span> <span className="text-rose-700">▼{movedDown}</span>
              </span>
            )
          )}
          <label className="relative ml-auto shrink-0">
            <span className="sr-only">Sort by</span>
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value as SortKey)}
              className="min-h-[44px] appearance-none rounded-xl bg-ink-50 py-2 pl-3 pr-8 text-sm font-medium text-ink-700 ring-1 ring-inset ring-ink-200 focus:outline-none focus:ring-2 focus:ring-brand-600 sm:min-h-[36px]"
            >
              {SORTS.map((x) => (
                <option key={x.key} value={x.key}>
                  {x.label}
                </option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
          </label>
        </div>
        {chips.length > 0 && (
          <div className="flex flex-wrap gap-1.5 px-4 pb-2">
            {chips.map((c) => (
              <button
                key={c.label}
                onClick={c.clear}
                className="inline-flex min-h-[28px] items-center gap-1 rounded-lg bg-ink-100 px-2 text-xs font-medium text-ink-700 hover:bg-ink-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 max-sm:min-h-[44px]"
                aria-label={`Remove filter ${c.label}`}
              >
                {c.label} <X className="h-3 w-3" aria-hidden />
              </button>
            ))}
          </div>
        )}
        {panel !== 'none' && (
          <div className="relative max-h-[60dvh] overflow-y-auto border-t border-ink-100 px-4 pb-4 pt-3">
            <button
              onClick={() => setPanel('none')}
              className="absolute right-2 top-1 flex h-11 w-11 items-center justify-center rounded-xl text-ink-400 hover:text-ink-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
              aria-label="Close panel"
            >
              <X className="h-4 w-4" />
            </button>
            {panel === 'filters' ? (
              <FilterPanel
                value={s.filters}
                extra={s.extraFilters}
                towns={towns}
                onApply={(f, x) => {
                  s.setExtraFilters(x);
                  s.setFilters(f);
                }}
                onClose={() => setPanel('none')}
              />
            ) : (
              <>
                <p className="mb-3 pr-10 text-sm text-ink-600">
                  Livability is half of every match. Move a slider and the list re-ranks; each card then shows how far it moved and why.
                </p>
                <div className="mb-4">
                  <PriorityPresets weights={s.weights} onPick={(w) => s.applyWeights(w)} busy={s.refreshing} />
                </div>
                <LivabilitySliders weights={s.weights} onCommit={(f, v) => s.adjustWeight(f, v)} onReset={s.resetWeights} compact busy={s.refreshing} />
              </>
            )}
          </div>
        )}
      </div>

      <div className="space-y-3 p-3 sm:p-4">
        <WarningBanner warnings={r.warnings} />
        {!phone && shortVersion}

        {baseline && priorityChanges.length > 0 && (
          <div className="rounded-xl bg-brand-50 px-3.5 py-3 text-[13px] text-brand-950 ring-1 ring-inset ring-brand-200" role="status">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <div className="font-semibold">Re-ranked for your new priorities</div>
                <div className="mt-0.5 text-brand-900/80">{priorityChanges.join(' · ')}</div>
              </div>
              <button
                onClick={() => s.setRankBaseline(null)}
                className="-mr-2 -mt-2 flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-brand-800 hover:bg-brand-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
                aria-label="Hide rank changes"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <ul className="mt-2 space-y-1">
              <li>
                <b className="text-emerald-800">▲ {movedUp}</b> moved up, <b className="text-rose-800">▼ {movedDown}</b> moved down, {shown - movedUp - movedDown} stayed.
              </li>
              {newTop && (
                <li>
                  {newTop.item.flat.id === baseline.topId ? (
                    <>
                      #1 is unchanged: <b>{newTop.item.flat.block} {titleCase(newTop.item.flat.street)}</b>.
                    </>
                  ) : (
                    <>
                      New #1: <b>{newTop.item.flat.block} {titleCase(newTop.item.flat.street)}</b>
                      {newTop.move != null && <> (was #{newTop.rank + newTop.move})</>}
                      {newTop.moveReason && <>. {newTop.moveReason}</>}.
                    </>
                  )}
                </li>
              )}
              {climber && (climber.move ?? 0) > 0 && climber !== newTop && (
                <li>
                  Biggest climb: <b>{climber.item.flat.block} {titleCase(climber.item.flat.street)}</b>, #{climber.rank + (climber.move ?? 0)} → #{climber.rank}.
                </li>
              )}
            </ul>
          </div>
        )}
        {sort !== 'match' && <p className="text-xs text-ink-500">Sorted by {SORTS.find((x) => x.key === sort)!.label.toLowerCase()}. The # on each card is still its match rank.</p>}

        {sorted.length === 0 ? (
          <EmptyState icon={<SearchX className="h-6 w-6" />} title="No matching flats">
            {r.results.length > 0 && ranking.rows.length === 0 ? (
              <ul className="mt-2 space-y-1 text-left">
                {ex.minLease != null && <li>• Lower the minimum lease (now {ex.minLease} years).</li>}
                {ex.minArea != null && <li>• Lower the minimum floor area (now {ex.minArea} m²).</li>}
              </ul>
            ) : r.suggestions.length > 0 ? (
              <ul className="mt-2 space-y-1 text-left">
                {r.suggestions.map((x) => (
                  <li key={x}>• {x}</li>
                ))}
              </ul>
            ) : (
              <p>Try relaxing your filters.</p>
            )}
            <div className="mt-4 flex justify-center gap-2">
              {chips.length > 0 && (
                <Button variant="secondary" onClick={clearAll}>
                  Clear filters
                </Button>
              )}
              <Button onClick={() => navigate('/')}>Edit search</Button>
            </div>
          </EmptyState>
        ) : (
          <>
            {sorted.slice(0, limit).map((row, i) => (
              <Fragment key={row.item.flat.id}>
                <RankCard
                  row={row}
                  avg={ranking.avg}
                  bookmarked={bookmarkIds.has(row.item.flat.id)}
                  selected={s.selectedFlatId === row.item.flat.id}
                  hovered={s.hoveredFlatId === row.item.flat.id}
                  onSelect={select}
                  onHover={hover}
                  onOpen={open}
                  onBookmark={bookmark}
                />
                {/* On a phone the summary waits below the first three cards so the top match stays above the fold. */}
                {phone && i === 2 && shortVersion}
              </Fragment>
            ))}
            {limit < sorted.length && (
              <Button variant="secondary" className="w-full" onClick={() => setLimit(limit + PAGE)}>
                Show more ({sorted.length - limit} left)
              </Button>
            )}
            <p className="pb-28 pt-2 text-center text-xs text-ink-500 md:pb-2">
              Match = 50% commute + 50% Livability Index. Prices are the most recent resale transaction and do not affect the rank.
            </p>
          </>
        )}
      </div>
    </div>
  );

  // Phone map: a bottom sheet that steps through the ranking.
  const sheetRow = ranking.byId.get(s.selectedFlatId ?? -1) ?? ranking.rows[0];
  const step = (d: number) => {
    if (!sheetRow) return;
    const next = ranking.rows[sheetRow.rank - 1 + d];
    if (next) s.setSelected(next.item.flat.id);
  };
  const sheet = sheetRow && (
    <div className="absolute inset-x-2 bottom-[68px] z-20 rounded-2xl bg-white p-3 shadow-pop ring-1 ring-ink-900/10 md:hidden">
      <div className="flex items-start gap-2">
        <div className="w-10 shrink-0 text-center">
          <div className="text-xl font-extrabold leading-none text-brand-700">#{sheetRow.rank}</div>
          <div className="mt-1">
            <MoveBadge move={sheetRow.move} />
          </div>
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-semibold">
            {sheetRow.item.flat.block} {titleCase(sheetRow.item.flat.street)}
          </div>
          <div className="truncate text-xs text-ink-500">
            {titleCase(sheetRow.item.flat.town)} · {flatTypeLabel(sheetRow.item.flat.flatType)} · <b className="text-ink-800">{moneyShort(sheetRow.item.flat.price)}</b>
          </div>
        </div>
        <div className="text-right">
          <div className="text-xl font-bold leading-none tabular-nums">{Math.round(sheetRow.item.matchScore)}</div>
          <div className="text-[10px] font-semibold uppercase text-ink-400">match</div>
        </div>
      </div>
      <ScoreStack parts={sheetRow.parts} className="mt-2" height={8} />
      {sheetRow.helps && <p className="mt-1.5 truncate text-xs text-ink-700"><b className="text-emerald-700">+</b> {sheetRow.helps.text}</p>}
      {sheetRow.hurts && <p className="truncate text-xs text-ink-700"><b className="text-rose-700">−</b> {sheetRow.hurts.text}</p>}
      <div className="mt-2 flex gap-2">
        <button onClick={() => step(-1)} disabled={sheetRow.rank === 1} className="flex h-11 w-11 items-center justify-center rounded-xl ring-1 ring-inset ring-ink-200 disabled:opacity-40" aria-label="Previous rank">
          <ChevronLeft className="h-5 w-5" />
        </button>
        <button onClick={() => step(1)} disabled={sheetRow.rank === ranking.rows.length} className="flex h-11 w-11 items-center justify-center rounded-xl ring-1 ring-inset ring-ink-200 disabled:opacity-40" aria-label="Next rank">
          <ChevronRight className="h-5 w-5" />
        </button>
        <Button className="flex-1" onClick={() => open(sheetRow.item.flat.id)}>
          Details <ArrowRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );

  return (
    <div className="relative h-[calc(100dvh-64px)] md:grid md:grid-cols-[minmax(400px,500px)_1fr] xl:grid-cols-[540px_1fr]">
      <div className={`${view === 'list' ? 'block' : 'hidden'} h-full overflow-hidden border-r border-ink-900/5 md:block`}>{listPane}</div>
      <div className={`${view === 'map' ? 'block' : 'hidden'} relative h-full md:block`}>
        <MapView
          rows={ranking.rows}
          anchors={r.anchors}
          hoveredId={s.hoveredFlatId}
          selectedId={s.selectedFlatId}
          onHover={hover}
          onSelect={selectFromMap}
          onOpen={open}
          padBottom={phone ? 210 : 0}
        />
        {view === 'map' && sheet}
        {baseline && priorityChanges.length > 0 && (
          <div className="pointer-events-none absolute bottom-3 left-3 hidden items-center gap-1.5 rounded-xl bg-white/95 px-3 py-2 text-xs text-ink-700 shadow-card ring-1 ring-ink-900/5 md:flex">
            <RotateCcw className="h-3.5 w-3.5 text-brand-700" aria-hidden /> Numbered pins show the new top 10
          </div>
        )}
      </div>
      <button
        onClick={() => setView(view === 'list' ? 'map' : 'list')}
        className={`fixed z-30 inline-flex min-h-[44px] items-center gap-2 rounded-full bg-ink-900 px-5 text-sm font-semibold text-white shadow-pop focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 focus-visible:ring-offset-2 md:hidden ${
          view === 'map' ? 'right-3 top-[76px]' : 'bottom-20 left-1/2 -translate-x-1/2'
        }`}
      >
        {view === 'list' ? <MapIcon className="h-4 w-4" /> : <List className="h-4 w-4" />}
        {view === 'list' ? 'Map' : 'List'}
      </button>
    </div>
  );
}
