import { GitCompare, Star, Trash2 } from '../components/icons';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, ApiError } from '../api/apiClient';
import { ScoreRing } from '../components/ScoreRing';
import { ValuationBadge } from '../components/ValuationBadge';
import { Button, Card, EmptyState, Spinner } from '../components/ui';
import { flatTypeLabel, money, monthLabel, titleCase } from '../lib/format';
import { useBookmarks } from '../state/bookmarkStore';
import { toast } from '../state/toastStore';
import type { Bookmark } from '../types/models';

/** Bookmarks list (UC-08) and the starting point for Compare (UC-09). */
export default function BookmarksScreen() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [items, setItems] = useState<Bookmark[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { compare, toggleCompare, setCompare, toggle } = useBookmarks();

  useEffect(() => {
    api
      .bookmarks()
      .then((b) => {
        setItems(b);
        const pre = Number(params.get('select'));
        const ids = b.map((x) => x.flat.id);
        const kept = compare.filter((c) => ids.includes(c));
        setCompare(pre && ids.includes(pre) && !kept.includes(pre) ? [...kept, pre] : kept);
      })
      .catch((e: ApiError) => setError(e.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const remove = async (id: number) => {
    try {
      await toggle(id);
      setItems((list) => list?.filter((b) => b.flat.id !== id) ?? null);
      toast('Removed from bookmarks', 'success');
    } catch (e) {
      toast((e as Error).message, 'error');
    }
  };

  if (error) return <EmptyState icon={<Star className="h-6 w-6" />} title="Could not load bookmarks">{error}</EmptyState>;
  if (!items) return <Spinner label="Loading bookmarks" />;

  return (
    <div className="mx-auto max-w-6xl px-4 pb-32 pt-6 md:pb-16 lg:px-6">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-semibold tracking-tight">Bookmarks</h1>
          <p className="text-sm text-ink-500">Tick two or more flats to compare them side by side.</p>
        </div>
      </div>

      {items.length === 0 ? (
        <Card>
          <EmptyState icon={<Star className="h-6 w-6" />} title="No bookmarks yet">
            Tap the star on any flat to save it here.
            <div className="mt-4">
              <Button onClick={() => navigate('/')}>Find flats</Button>
            </div>
          </EmptyState>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((b) => {
            const selected = compare.includes(b.flat.id);
            return (
              <Card key={b.flat.id} className={`relative p-4 transition ${selected ? 'ring-2 ring-brand-500' : ''}`}>
                <label className="absolute left-4 top-4 flex cursor-pointer items-center gap-2 text-xs font-medium text-ink-600">
                  <input type="checkbox" checked={selected} onChange={() => toggleCompare(b.flat.id)} className="h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500" />
                  Compare
                </label>
                <button onClick={() => remove(b.flat.id)} className="absolute right-3 top-3 rounded-lg p-1.5 text-ink-400 hover:bg-rose-50 hover:text-rose-600" aria-label="Remove bookmark">
                  <Trash2 className="h-4 w-4" />
                </button>
                <Link to={`/flats/${b.flat.id}`} className="mt-7 block">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">{titleCase(b.flat.town)}</div>
                      <div className="truncate font-semibold">
                        {b.flat.block} {titleCase(b.flat.street)}
                      </div>
                      <div className="text-sm text-ink-500">
                        {flatTypeLabel(b.flat.flatType)} · {Math.round(b.flat.floorAreaSqm)} m² · {b.flat.remainingLeaseYears} yrs
                      </div>
                    </div>
                    <ScoreRing value={b.livability.index} size={46} label="Livability" />
                  </div>
                  <div className="mt-3 flex items-center justify-between">
                    <div>
                      <div className="text-lg font-bold">{money(b.flat.price)}</div>
                      <div className="text-xs text-ink-400">sold {monthLabel(b.flat.priceMonth)}</div>
                    </div>
                    <ValuationBadge label={b.valuation} size="sm" />
                  </div>
                </Link>
              </Card>
            );
          })}
        </div>
      )}

      {items.length > 0 && (
        <div className="fixed inset-x-0 bottom-16 z-30 border-t border-ink-900/10 bg-white/95 px-4 py-3 backdrop-blur md:bottom-0">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-3">
            <span className="text-sm text-ink-600">{compare.length < 2 ? `Select ${2 - compare.length} more to compare` : `${compare.length} flats selected`}</span>
            <Button disabled={compare.length < 2} onClick={() => navigate('/compare')}>
              <GitCompare className="h-4 w-4" /> Compare {compare.length >= 2 ? compare.length : ''}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
