import { useEffect, useState } from 'react';
import { api } from '../api/apiClient';
import type { Valuation } from '../types/models';

const cache = new Map<number, Valuation | null>();
const inflight = new Map<number, Promise<Valuation | null>>();

function load(id: number): Promise<Valuation | null> {
  if (cache.has(id)) return Promise.resolve(cache.get(id)!);
  let p = inflight.get(id);
  if (!p) {
    p = api
      .valuation(id)
      .then((v) => v)
      .catch(() => null)
      .then((v) => {
        cache.set(id, v);
        inflight.delete(id);
        return v;
      });
    inflight.set(id, p);
  }
  return p;
}

/** The fair-value estimate for one flat, fetched once and shared by cards, map and detail. */
export function useValuation(id: number | null, enabled = true): Valuation | null {
  const [v, setV] = useState<Valuation | null>(() => (id != null ? cache.get(id) ?? null : null));
  useEffect(() => {
    if (id == null || !enabled) return;
    let live = true;
    if (cache.has(id)) setV(cache.get(id) ?? null);
    else load(id).then((x) => live && setV(x));
    return () => {
      live = false;
    };
  }, [id, enabled]);
  return v;
}

/** "23% above fair value (S$472k)", or null when there is no estimate. */
export function fairGap(v: Valuation | null): { pct: number; diff: number } | null {
  if (!v || v.fairValue == null || v.price == null || v.fairValue === 0) return null;
  return { pct: ((v.price - v.fairValue) / v.fairValue) * 100, diff: v.price - v.fairValue };
}
