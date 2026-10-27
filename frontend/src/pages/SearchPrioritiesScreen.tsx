import { Bike, Bus, Car, Footprints, Info, Plus, Search, X } from '../components/icons';
import { useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/apiClient';
import { LivabilitySliders } from '../components/LivabilitySliders';
import { MultiSelect } from '../components/MultiSelect';
import { PlaceInput } from '../components/PlaceInput';
import { PriorityPresets } from '../components/PriorityPresets';
import { RankingFormula } from '../components/RankingFormula';
import { Button, Card, Chip, Field, inputClass } from '../components/ui';
import { flatTypeLabel, titleCase } from '../lib/format';
import { useSearch } from '../state/searchStore';
import type { TransportMode } from '../types/models';

const MODES: { mode: TransportMode; label: string; icon: typeof Bus }[] = [
  { mode: 'PUBLIC_TRANSPORT', label: 'Public transport', icon: Bus },
  { mode: 'DRIVE', label: 'Drive', icon: Car },
  { mode: 'CYCLE', label: 'Cycle', icon: Bike },
  { mode: 'WALK', label: 'Walk', icon: Footprints },
];

function Step({ n, title, sub, children }: { n: number; title: string; sub?: ReactNode; children: ReactNode }) {
  return (
    <section className="relative pl-10">
      <span className="absolute left-0 top-0 flex h-7 w-7 items-center justify-center rounded-full bg-ink-900 text-[13px] font-bold text-white" aria-hidden>
        {n}
      </span>
      <h2 className="text-[15px] font-semibold leading-7 text-ink-900">{title}</h2>
      {sub && <p className="-mt-0.5 mb-3 text-[13px] text-ink-500">{sub}</p>}
      <div className={sub ? '' : 'mt-2'}>{children}</div>
    </section>
  );
}

/** UC-01 search form with UC-03 livability priorities, next to a live picture of how flats will be ranked. */
export default function SearchPrioritiesScreen() {
  const navigate = useNavigate();
  const { form, setForm, weights, adjustWeight, resetWeights, applyWeights, runSearch, loading, error, fieldErrors, searchId, setExtraFilters, setRankBaseline } = useSearch();
  const [options, setOptions] = useState<{ towns: string[]; flatTypes: string[] }>({ towns: [], flatTypes: [] });
  const [showSecond, setShowSecond] = useState(form.workplace2 !== '');

  useEffect(() => {
    api.options().then(setOptions).catch(() => undefined);
  }, []);

  const toggle = <T,>(list: T[], v: T) => (list.includes(v) ? list.filter((x) => x !== v) : [...list, v]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (await runSearch()) {
      setExtraFilters({ minLease: null, minArea: null });
      setRankBaseline(null);
      navigate('/results');
    }
  };

  const workplaces = [form.workplace1.trim() || 'Workplace 1', ...(showSecond ? [form.workplace2.trim() || 'Workplace 2'] : [])];

  return (
    <div className="mx-auto max-w-6xl px-4 pb-28 pt-6 md:pb-16 md:pt-10 lg:px-6">
      <div className="mb-6 max-w-3xl md:mb-8">
        <p className="text-sm font-semibold text-brand-700">HDB resale, ranked and explained</p>
        <h1 className="mt-1 font-display text-[30px] font-semibold leading-[1.1] tracking-tight text-ink-950 md:text-[44px]">Find a resale flat that works for both of you.</h1>
        <p className="mt-2 text-[15px] text-ink-600 md:text-base">
          Tell us where you work and what matters. Every flat gets a match score out of 100, and we show exactly where each point came from.
        </p>
      </div>

      <form onSubmit={submit} className="grid items-start gap-5 lg:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)]" noValidate>
        <Card className="p-5 md:p-7">
          <div className="space-y-7">
            <Step n={1} title="Where do you work?" sub="Half of every match score is commute. Each workplace counts equally.">
              <div className="grid gap-4 md:grid-cols-2">
                <Field label="Workplace 1" htmlFor="workplace1" error={fieldErrors.workplace1} hint="An address, building, area or postal code">
                  <PlaceInput id="workplace1" value={form.workplace1} onChange={(v) => setForm({ workplace1: v })} placeholder="e.g. Raffles Place" error={fieldErrors.workplace1} autoFocus={typeof window !== 'undefined' && window.matchMedia('(min-width: 768px)').matches} />
                </Field>
                {showSecond ? (
                  <Field
                    label={
                      <span className="flex items-center justify-between">
                        <span>
                          Workplace 2 <span className="font-normal text-ink-400">(optional)</span>
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            setShowSecond(false);
                            setForm({ workplace2: '' });
                          }}
                          className="-my-3 -mr-3 flex h-11 w-11 items-center justify-center rounded-xl text-ink-400 hover:text-ink-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 sm:-my-2 sm:h-8 sm:w-8"
                          aria-label="Remove Workplace 2"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </span>
                    }
                    htmlFor="workplace2"
                    error={fieldErrors.workplace2}
                    hint="Your partner's office, or your parents' home"
                  >
                    <PlaceInput id="workplace2" value={form.workplace2} onChange={(v) => setForm({ workplace2: v })} placeholder="e.g. one-north" error={fieldErrors.workplace2} />
                  </Field>
                ) : (
                  <div className="flex items-start md:pt-[26px]">
                    <button
                      type="button"
                      onClick={() => setShowSecond(true)}
                      className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl border border-dashed border-ink-300 text-sm font-medium text-ink-600 hover:border-brand-400 hover:text-brand-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
                    >
                      <Plus className="h-4 w-4" /> Add a second workplace
                    </button>
                  </div>
                )}
              </div>
            </Step>

            <Step n={2} title="How do you get there?" sub="Pick one or more. Each flat uses the fastest of them.">
              <div className="flex flex-wrap gap-2" role="group" aria-label="Transport modes">
                {MODES.map(({ mode, label, icon: Icon }) => (
                  <Chip
                    key={mode}
                    active={form.transportModes.includes(mode)}
                    onClick={() => {
                      const next = toggle(form.transportModes, mode);
                      setForm({ transportModes: next.length ? next : [mode] });
                    }}
                    icon={<Icon className="h-4 w-4" />}
                  >
                    {label}
                  </Chip>
                ))}
              </div>
            </Step>

            <Step n={3} title="Narrow it down" sub="Optional. These filter flats out; they never change a score.">
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Min price" htmlFor="minPrice" error={fieldErrors.minPrice}>
                    <div className="relative">
                      <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm text-ink-400">S$</span>
                      <input id="minPrice" inputMode="numeric" value={form.minPrice} onChange={(e) => setForm({ minPrice: e.target.value.replace(/[^\d]/g, '') })} placeholder="No min" className={`${inputClass()} pl-10`} />
                    </div>
                  </Field>
                  <Field label="Max price" htmlFor="maxPrice" error={fieldErrors.maxPrice}>
                    <div className="relative">
                      <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm text-ink-400">S$</span>
                      <input id="maxPrice" inputMode="numeric" value={form.maxPrice} onChange={(e) => setForm({ maxPrice: e.target.value.replace(/[^\d]/g, '') })} placeholder="No max" className={`${inputClass(fieldErrors.maxPrice)} pl-10`} />
                    </div>
                  </Field>
                </div>
                <div>
                  <div className="mb-2 text-sm font-medium text-ink-800">Flat type</div>
                  <div className="flex flex-wrap gap-2" role="group" aria-label="Flat type">
                    {(options.flatTypes.length ? options.flatTypes : ['2 ROOM', '3 ROOM', '4 ROOM', '5 ROOM', 'EXECUTIVE']).map((t) => (
                      <Chip key={t} active={form.flatTypes.includes(t)} onClick={() => setForm({ flatTypes: toggle(form.flatTypes, t) })}>
                        {flatTypeLabel(t)}
                      </Chip>
                    ))}
                  </div>
                </div>
                <Field label="Towns" hint="Leave empty to search every town. Lease and floor-area filters are on the results.">
                  <MultiSelect options={options.towns} value={form.towns} onChange={(towns) => setForm({ towns })} placeholder="Any town" format={titleCase} />
                </Field>
              </div>
            </Step>

            {error && !Object.keys(fieldErrors).length && (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700" role="alert">
                {error}
              </p>
            )}

            <div className="flex flex-col-reverse gap-3 border-t border-ink-100 pt-5 sm:flex-row sm:items-center sm:justify-between">
              {searchId ? (
                <button type="button" onClick={() => navigate('/results')} className="min-h-[44px] rounded-lg text-sm font-medium text-brand-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600">
                  Back to your last results
                </button>
              ) : (
                <span />
              )}
              <Button type="submit" size="lg" loading={loading} className="w-full sm:w-auto">
                <Search className="h-5 w-5" /> Search flats
              </Button>
            </div>
          </div>
        </Card>

        <div className="lg:sticky lg:top-20">
          <Card className="p-5 md:p-6">
            <h2 className="text-[15px] font-semibold text-ink-900">How each flat will be scored</h2>
            <p className="mb-4 mt-0.5 text-[13px] text-ink-500">100 points in total. This bar updates as you type and drag.</p>
            <RankingFormula weights={weights} workplaces={workplaces} />

            <div className="mt-5 border-t border-ink-100 pt-4">
              <h3 className="text-sm font-semibold text-ink-900">4. What matters most at home?</h3>
              <p className="mb-4 mt-0.5 text-[13px] text-ink-500">Pick a starting point, or split the 50 livability points yourself. You can change this later and watch the ranking move.</p>
              <div className="mb-4">
                <PriorityPresets weights={weights} onPick={(w) => applyWeights(w)} />
              </div>
              <LivabilitySliders weights={weights} onCommit={(f, v) => adjustWeight(f, v)} onReset={resetWeights} />
            </div>

            <div className="mt-5 flex gap-2.5 rounded-xl bg-amber-50 px-3.5 py-3 text-[13px] leading-relaxed text-amber-950 ring-1 ring-inset ring-amber-200">
              <Info className="mt-0.5 h-4 w-4 shrink-0 text-amber-700" aria-hidden />
              <p>
                <b>Price is not in the score.</b> Each result shows its price against a fair-value estimate separately, so a great match is never hidden just because it costs more.
              </p>
            </div>
          </Card>
        </div>
      </form>
    </div>
  );
}
