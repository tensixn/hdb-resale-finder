import { useEffect, useState } from 'react';
import { flatTypeLabel, titleCase } from '../lib/format';
import type { ExtraFilters } from '../state/searchStore';
import type { Filters } from '../types/models';
import { MultiSelect } from './MultiSelect';
import { Button, Chip, Field, inputClass } from './ui';

/** UC-10: narrow the stored result set by price, flat type, town, remaining lease and floor area. */
export function FilterPanel({
  value,
  extra,
  towns,
  onApply,
  onClose,
}: {
  value: Filters;
  extra: ExtraFilters;
  towns: string[];
  onApply: (f: Filters, extra: ExtraFilters) => void;
  onClose: () => void;
}) {
  const [min, setMin] = useState(value.minPrice?.toString() ?? '');
  const [max, setMax] = useState(value.maxPrice?.toString() ?? '');
  const [types, setTypes] = useState(value.flatTypes);
  const [selTowns, setSelTowns] = useState(value.towns);
  const [lease, setLease] = useState(extra.minLease?.toString() ?? '');
  const [area, setArea] = useState(extra.minArea?.toString() ?? '');
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    setMin(value.minPrice?.toString() ?? '');
    setMax(value.maxPrice?.toString() ?? '');
    setTypes(value.flatTypes);
    setSelTowns(value.towns);
  }, [value]);
  useEffect(() => {
    setLease(extra.minLease?.toString() ?? '');
    setArea(extra.minArea?.toString() ?? '');
  }, [extra]);

  const num = (s: string) => (s.trim() ? Number(s) : null);
  const apply = () => {
    const lo = num(min);
    const hi = num(max);
    if (lo != null && hi != null && lo > hi) {
      setError('Min price must be below max price.');
      return;
    }
    const l = num(lease);
    if (l != null && l > 99) {
      setError('A lease has at most 99 years.');
      return;
    }
    setError(null);
    onApply({ minPrice: lo, maxPrice: hi, flatTypes: types, towns: selTowns }, { minLease: l, minArea: num(area) });
    onClose();
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <Field label="Min price (S$)" htmlFor="f-min">
          <input id="f-min" inputMode="numeric" value={min} onChange={(e) => setMin(e.target.value.replace(/[^\d]/g, ''))} placeholder="Any" className={inputClass()} />
        </Field>
        <Field label="Max price (S$)" htmlFor="f-max">
          <input id="f-max" inputMode="numeric" value={max} onChange={(e) => setMax(e.target.value.replace(/[^\d]/g, ''))} placeholder="Any" className={inputClass()} />
        </Field>
        <Field label="Min lease left (yrs)" htmlFor="f-lease">
          <input id="f-lease" inputMode="numeric" value={lease} onChange={(e) => setLease(e.target.value.replace(/[^\d]/g, ''))} placeholder="Any" className={inputClass()} />
        </Field>
        <Field label="Min floor area (m²)" htmlFor="f-area">
          <input id="f-area" inputMode="numeric" value={area} onChange={(e) => setArea(e.target.value.replace(/[^\d]/g, ''))} placeholder="Any" className={inputClass()} />
        </Field>
      </div>
      <div>
        <div className="mb-2 text-sm font-medium text-ink-800">Flat type</div>
        <div className="flex flex-wrap gap-2">
          {['2 ROOM', '3 ROOM', '4 ROOM', '5 ROOM', 'EXECUTIVE'].map((t) => (
            <Chip key={t} active={types.includes(t)} onClick={() => setTypes(types.includes(t) ? types.filter((x) => x !== t) : [...types, t])}>
              {flatTypeLabel(t)}
            </Chip>
          ))}
        </div>
      </div>
      <Field label="Towns">
        <MultiSelect options={towns} value={selTowns} onChange={setSelTowns} placeholder="Any town" format={titleCase} />
      </Field>
      {error && (
        <p className="text-sm text-rose-700" role="alert">
          {error}
        </p>
      )}
      <div className="flex items-center justify-between pt-1">
        <button
          type="button"
          className="min-h-[44px] rounded-lg px-1 text-sm font-medium text-ink-500 hover:text-ink-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
          onClick={() => {
            onApply({ minPrice: null, maxPrice: null, flatTypes: [], towns: [] }, { minLease: null, minArea: null });
            onClose();
          }}
        >
          Clear all
        </button>
        <Button onClick={apply}>Apply filters</Button>
      </div>
    </div>
  );
}
