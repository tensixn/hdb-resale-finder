import { ArrowLeft, Crown, X } from '../components/icons';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, ApiError } from '../api/apiClient';
import { MODE_ICON } from '../components/FlatCard';
import { ScoreRing } from '../components/ScoreRing';
import { ValuationBadge } from '../components/ValuationBadge';
import { Button, Card, EmptyState, Spinner } from '../components/ui';
import { flatTypeLabel, money, titleCase } from '../lib/format';
import { useBookmarks } from '../state/bookmarkStore';
import { useSearch } from '../state/searchStore';
import type { CompareResult, CompareRow } from '../types/models';

/** UC-09: bookmarked flats side by side, with the best value in each row highlighted. */
export default function CompareScreen() {
  const navigate = useNavigate();
  const { compare, toggleCompare } = useBookmarks();
  const { searchId, weights } = useSearch();
  const [data, setData] = useState<CompareResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (compare.length < 2) return;
    api
      .compare(compare, searchId, weights)
      .then(setData)
      .catch((e: ApiError) => setError(e.message));
  }, [compare, searchId, weights]);

  if (compare.length < 2) {
    return (
      <EmptyState icon={<Crown className="h-6 w-6" />} title="Pick at least two flats">
        Select two or more bookmarks to compare.
        <div className="mt-4">
          <Button onClick={() => navigate('/bookmarks')}>Go to bookmarks</Button>
        </div>
      </EmptyState>
    );
  }
  if (error) return <EmptyState icon={<Crown className="h-6 w-6" />} title="Could not compare">{error}</EmptyState>;
  if (!data) return <Spinner label="Comparing" />;

  const best = (metric: string, row: CompareRow) => data.best[metric] === row.flat.id;
  const cell = (metric: string, row: CompareRow, content: React.ReactNode) => (
    <td key={row.flat.id} className={`px-4 py-3 align-top ${best(metric, row) ? 'bg-emerald-50/70' : ''}`}>
      <div className="flex items-center gap-1.5">
        {content}
        {best(metric, row) && <Crown className="h-3.5 w-3.5 shrink-0 text-emerald-600" aria-label="Best" />}
      </div>
    </td>
  );

  const rows: { label: string; metric?: string; render: (r: CompareRow) => React.ReactNode }[] = [
    { label: 'Price', metric: 'price', render: (r) => <b className="tabular-nums">{money(r.flat.price)}</b> },
    { label: 'Valuation', render: (r) => <ValuationBadge label={r.valuation} size="sm" /> },
    { label: 'Fair value', render: (r) => <span className="tabular-nums">{money(r.fairValue)}</span> },
    { label: 'Price per m²', metric: 'pricePerSqm', render: (r) => <span className="tabular-nums">{money(r.pricePerSqm)}</span> },
    { label: 'Floor area', metric: 'floorArea', render: (r) => `${Math.round(r.flat.floorAreaSqm)} m²` },
    { label: 'Lease left', metric: 'remainingLease', render: (r) => `${r.flat.remainingLeaseYears} years` },
    { label: 'Livability Index', metric: 'livability', render: (r) => <ScoreRing value={r.livability.index} size={40} label="Livability" /> },
    {
      label: 'Commute',
      metric: 'commute',
      render: (r) =>
        r.commute ? (
          <div className="space-y-1">
            {r.commute.map((l) => {
              const Icon = MODE_ICON[l.mode];
              return (
                <div key={l.anchorIndex} className="flex items-center gap-1.5 text-sm">
                  <span className={`flex h-4 w-4 items-center justify-center rounded text-[9px] font-bold text-white ${l.anchorIndex === 0 ? 'bg-ink-900' : 'bg-[#a8472f]'}`}>{l.anchorIndex + 1}</span>
                  <Icon className="h-3.5 w-3.5 text-ink-400" />
                  <b className="tabular-nums">{l.minutes}</b> min
                </div>
              );
            })}
          </div>
        ) : (
          <span className="text-xs text-ink-400">Not available (no search this session)</span>
        ),
    },
    { label: 'Lease score', render: (r) => Math.round(r.livability.lease) },
    { label: 'Safety score', render: (r) => Math.round(r.livability.safety) },
    { label: 'Space score', render: (r) => Math.round(r.livability.density) },
    { label: 'Air quality score', render: (r) => Math.round(r.livability.airQuality) },
  ];

  return (
    <div className="mx-auto max-w-6xl px-4 pb-28 pt-5 md:pb-16 lg:px-6">
      <button onClick={() => navigate('/bookmarks')} className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800">
        <ArrowLeft className="h-4 w-4" /> Bookmarks
      </button>
      <h1 className="mb-1 font-display text-3xl font-semibold tracking-tight">Compare flats</h1>
      <p className="mb-5 text-sm text-ink-500">
        <Crown className="mr-1 inline h-3.5 w-3.5 text-emerald-600" /> marks the best flat on each measure.
        {data.anchors.length > 0 && <> Commute is to {data.anchors.map((a) => a.label).join(' and ')}.</>}
      </p>
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[560px] text-sm">
          <thead>
            <tr className="border-b border-ink-100">
              <th className="w-40 px-4 py-4 text-left text-xs font-semibold uppercase tracking-wide text-ink-500" />
              {data.flats.map((r) => (
                <th key={r.flat.id} className="px-4 py-4 text-left align-top font-normal">
                  <div className="flex items-start justify-between gap-2">
                    <Link to={`/flats/${r.flat.id}`} className="min-w-0 hover:underline">
                      <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">{titleCase(r.flat.town)}</div>
                      <div className="font-semibold text-ink-900">
                        {r.flat.block} {titleCase(r.flat.street)}
                      </div>
                      <div className="text-xs text-ink-500">{flatTypeLabel(r.flat.flatType)}</div>
                    </Link>
                    {data.flats.length > 2 && (
                      <button onClick={() => toggleCompare(r.flat.id)} className="text-ink-300 hover:text-ink-600" aria-label="Remove from comparison">
                        <X className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {rows.map((row) => (
              <tr key={row.label}>
                <th className="px-4 py-3 text-left text-xs font-medium text-ink-500">{row.label}</th>
                {data.flats.map((r) =>
                  row.metric ? (
                    cell(row.metric, r, row.render(r))
                  ) : (
                    <td key={r.flat.id} className="px-4 py-3 align-top">
                      {row.render(r)}
                    </td>
                  ),
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
