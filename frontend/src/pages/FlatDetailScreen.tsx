import { ArrowLeft, Calculator, CalendarClock, ChevronDown, GitCompare, GraduationCap, MapPin, Minus, Plus, Scale, Star, TrendingUp } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, ApiError } from '../api/apiClient';
import { FairValueBar, PriceHistory } from '../components/Charts';
import { MODE_ICON } from '../components/FlatCard';
import { MoveBadge } from '../components/RankCard';
import { ScoreStack, Swatch } from '../components/ScoreStack';
import { ValuationBadge } from '../components/ValuationBadge';
import { WarningBanner } from '../components/WarningBanner';
import { Button, Card, EmptyState, InfoTip, SectionTitle, Spinner } from '../components/ui';
import { PART_COLOR, pts, type Part } from '../lib/explain';
import { dateLabel, flatTypeLabel, MODE_LABEL, money, moneyShort, monthLabel, titleCase, VALUATION } from '../lib/format';
import { useBookmarkAction } from '../lib/useBookmarkAction';
import { useRanking } from '../lib/useRanking';
import { useAuth } from '../state/authStore';
import { useBookmarks } from '../state/bookmarkStore';
import { useSearch } from '../state/searchStore';
import { toast } from '../state/toastStore';
import type { FlatDetail, Valuation } from '../types/models';
import { CPFCalculatorPanel } from './CPFCalculatorPanel';

function tier(rank: number, total: number): string {
  const p = rank / Math.max(1, total);
  if (rank <= 3 || p <= 0.05) return 'One of your best matches';
  if (p <= 0.25) return 'A strong match';
  if (p <= 0.6) return 'A middling match';
  return 'A weaker match';
}

function priceVerdict(v: Valuation): { short: string; long: string; tone: 'good' | 'ok' | 'bad' | 'none' } {
  const gap = v.fairValue != null && v.price != null ? ((v.price - v.fairValue) / v.fairValue) * 100 : null;
  switch (v.label) {
    case 'UNDER_VALUED':
      return { short: 'priced below fair value', long: `${money(v.price)} is ${Math.abs(gap ?? 0).toFixed(0)}% under its fair value of ${money(v.fairValue)}.`, tone: 'good' };
    case 'FAIR_VALUED':
      return { short: 'fairly priced', long: `${money(v.price)} is within ${v.fairBandPercent}% of its fair value of ${money(v.fairValue)}.`, tone: 'ok' };
    case 'OVER_VALUED':
      return { short: 'priced above fair value', long: `${money(v.price)} is ${Math.abs(gap ?? 0).toFixed(0)}% over its fair value of ${money(v.fairValue)}, about ${money(Math.abs((v.price ?? 0) - (v.fairValue ?? 0)))} more.`, tone: 'bad' };
    default:
      return { short: 'with no fair-value estimate', long: v.message ?? 'Not enough recent sales of this flat type in this town to estimate a fair value.', tone: 'none' };
  }
}

/** One part of the match score: what was measured, its 0-100 score, and the points it earned. */
function PointsRow({ part, detail, delta }: { part: Part; detail: string; delta?: number }) {
  return (
    <li className="py-2.5">
      <div className="flex items-baseline justify-between gap-3">
        <span className="flex min-w-0 items-center gap-2 text-sm font-medium text-ink-800">
          <Swatch k={part.key} />
          <span className="truncate">{part.kind === 'commute' ? `Commute to ${part.label}` : part.label}</span>
        </span>
        <span className="shrink-0 text-sm tabular-nums text-ink-500">
          <b className="text-ink-900">{part.points.toFixed(1)}</b> / {part.share.toFixed(part.share % 1 ? 1 : 0)} pts
        </span>
      </div>
      <div className="mt-1.5 h-2 overflow-hidden rounded-full" style={{ background: `${PART_COLOR[part.key]}22` }}>
        <div className="h-full rounded-full transition-all duration-500" style={{ width: `${part.score}%`, background: PART_COLOR[part.key] }} />
      </div>
      <div className="mt-1 flex items-start justify-between gap-3 text-xs text-ink-500">
        <span>
          Scores {Math.round(part.score)}/100 · {detail}
        </span>
        {delta != null && Math.abs(delta) >= 0.1 && (
          <span className={`shrink-0 font-semibold ${delta > 0 ? 'text-emerald-700' : 'text-rose-700'}`}>
            {delta > 0 ? '+' : '−'}
            {pts(delta)} vs avg
          </span>
        )}
      </div>
    </li>
  );
}

/** UC-04 View Flat Details, led by a plain-language verdict, with the UC-05 fair-value and UC-06 CPF extension points. */
export default function FlatDetailScreen() {
  const { id } = useParams();
  const flatId = Number(id);
  const navigate = useNavigate();
  const { searchId, weights, result } = useSearch();
  const ranking = useRanking();
  const email = useAuth((s) => s.email);
  const setPending = useAuth((s) => s.setPending);
  const bookmarkIds = useBookmarks((s) => s.ids);
  const toggleBookmark = useBookmarkAction();
  const [detail, setDetail] = useState<FlatDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cpfOpen, setCpfOpen] = useState(false);

  useEffect(() => {
    setDetail(null);
    api
      .flat(flatId, searchId, weights)
      .then(setDetail)
      .catch((e: ApiError) => setError(e.message));
    window.scrollTo(0, 0);
  }, [flatId, searchId, weights]);

  if (error) return <EmptyState icon={<MapPin className="h-6 w-6" />} title="Flat not found">{error}</EmptyState>;
  if (!detail) return <Spinner label="Loading flat details" />;

  const f = detail.flat;
  const v = detail.valuation;
  const row = ranking.byId.get(flatId);
  const total = ranking.rows.length;
  const bookmarked = bookmarkIds.has(flatId);
  const price = priceVerdict(v);

  // UC-09.AC.2: compare from here opens Bookmarks with this flat selected.
  const compare = async () => {
    if (!email) {
      setPending({ type: 'compare', flatId });
      navigate(`/login?next=${encodeURIComponent(`/bookmarks?select=${flatId}`)}`);
      return;
    }
    if (!bookmarkIds.has(flatId)) {
      await useBookmarks.getState().toggle(flatId);
      toast('Saved to bookmarks so you can compare it', 'success');
    }
    navigate(`/bookmarks?select=${flatId}`);
  };

  const factorDetail: Record<string, string> = {
    lease: `${f.remainingLeaseYears} of 99 years left (lease from ${f.leaseCommenceYear})`,
    safety: `${detail.context.crimeRatePerThousand.toFixed(1)} reported crimes per 1,000 residents in ${titleCase(f.town)}`,
    density: `${Math.round(detail.context.residentsPerHa)} residents per hectare`,
    airQuality: `24-hour PSI ${detail.context.psi ?? '–'} in the ${detail.region} region`,
  };
  const legDetail = (p: Part) => {
    const leg = detail.commute?.find((l) => (l.anchorIndex === 0 ? 'w0' : 'w1') === p.key);
    return leg ? `${leg.minutes} min by ${MODE_LABEL[leg.mode].toLowerCase()}` : p.raw;
  };
  const deltaOf = (p: Part) => {
    const a = ranking.avg[p.key];
    return a != null ? (p.share * (p.score - a)) / 100 : undefined;
  };

  const headline = row
    ? `${tier(row.rank, total)}${{ good: ', and ', ok: ', and ', bad: ', but ', none: ', ' }[price.tone]}${price.short}.`
    : `Livability ${Math.round(detail.livability.index)} out of 100, ${price.short}.`;
  const toneCls = { good: 'bg-emerald-50 ring-emerald-200', ok: 'bg-sky-50 ring-sky-200', bad: 'bg-rose-50 ring-rose-200', none: 'bg-ink-50 ring-ink-200' }[price.tone];

  return (
    <div className="mx-auto max-w-6xl px-4 pb-28 pt-4 md:pb-16 lg:px-6">
      <button
        onClick={() => (window.history.length > 1 ? navigate(-1) : navigate('/results'))}
        className="-ml-1 mb-3 inline-flex min-h-[44px] items-center gap-1.5 rounded-lg px-1 text-sm font-medium text-ink-500 hover:text-ink-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
      >
        <ArrowLeft className="h-4 w-4" /> Back to results
      </button>

      <div className="mb-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="min-w-0">
          <div className="mb-1 flex flex-wrap items-center gap-2 text-sm text-ink-500">
            <MapPin className="h-4 w-4" /> {titleCase(f.town)} · {detail.region}
          </div>
          <h1 className="text-2xl font-bold tracking-tight md:text-3xl">
            Blk {f.block} {titleCase(f.street)}
          </h1>
          <p className="mt-1 text-ink-600">
            {flatTypeLabel(f.flatType)} · {Math.round(f.floorAreaSqm)} m² · Storey {f.storeyRange.toLowerCase()} · {f.remainingLeaseYears} years lease left
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant={bookmarked ? 'secondary' : 'primary'} onClick={() => toggleBookmark(flatId)} className="flex-1 md:flex-none">
            <Star className={`h-4 w-4 ${bookmarked ? 'text-amber-500' : ''}`} fill={bookmarked ? 'currentColor' : 'none'} />
            {bookmarked ? 'Bookmarked' : 'Bookmark'}
          </Button>
          <Button variant="secondary" onClick={compare} className="flex-1 md:flex-none">
            <GitCompare className="h-4 w-4" /> Compare
          </Button>
        </div>
      </div>

      {detail.warnings.length > 0 && (
        <div className="mb-5">
          <WarningBanner warnings={detail.warnings} />
        </div>
      )}

      {/* The verdict */}
      <Card className="mb-5 overflow-hidden">
        <div className="grid gap-0 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
          <div className="p-5 md:p-6">
            <div className="flex flex-wrap items-center gap-2 text-xs font-semibold uppercase tracking-wide text-brand-700">
              Our verdict
              {row && (
                <span className="rounded-full bg-brand-50 px-2 py-0.5 normal-case tracking-normal text-brand-800 ring-1 ring-inset ring-brand-200">
                  #{row.rank} of {total} in your results
                </span>
              )}
              {row && <MoveBadge move={row.move} size="md" />}
            </div>
            <p className="mt-2 text-xl font-semibold leading-snug text-ink-950 md:text-2xl">{headline}</p>
            {row ? (
              <ul className="mt-4 space-y-2 text-[15px] text-ink-800">
                {row.helps && (
                  <li className="flex gap-2.5">
                    <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-700" aria-hidden>
                      <Plus className="h-3.5 w-3.5" strokeWidth={3} />
                    </span>
                    <span>
                      <b>Best thing:</b> {row.helps.text}, worth {pts(row.helps.delta)} more points than the typical flat in your results.
                    </span>
                  </li>
                )}
                {row.hurts && (
                  <li className="flex gap-2.5">
                    <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-rose-100 text-rose-700" aria-hidden>
                      <Minus className="h-3.5 w-3.5" strokeWidth={3} />
                    </span>
                    <span>
                      <b>Trade-off:</b> {row.hurts.text}, costing it {pts(row.hurts.delta)} points.
                    </span>
                  </li>
                )}
                <li className="flex gap-2.5">
                  <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-ink-100 text-ink-600" aria-hidden>
                    <Scale className="h-3.5 w-3.5" />
                  </span>
                  <span>
                    <b>Price:</b> {price.long}
                  </span>
                </li>
                {row.vs && <li className="border-l-2 border-ink-200 pl-3 text-sm text-ink-600">{row.vs.text}</li>}
                {row.move != null && row.move !== 0 && row.moveReason && (
                  <li className="border-l-2 border-ink-200 pl-3 text-sm text-ink-600">
                    {row.move > 0 ? `Up ${row.move}` : `Down ${-row.move}`} since you changed priorities: {row.moveReason}.
                  </li>
                )}
              </ul>
            ) : (
              <p className="mt-3 text-[15px] text-ink-700">
                {price.long}{' '}
                {result ? 'This flat is not in your current results, so it has no match rank.' : 'Run a search to see its commute and where it ranks for you.'}
              </p>
            )}
          </div>

          <div className="border-t border-ink-100 bg-ink-50/60 p-5 md:p-6 lg:border-l lg:border-t-0">
            {row ? (
              <>
                <div className="flex items-end justify-between">
                  <div>
                    <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Match score</div>
                    <div className="text-5xl font-bold leading-none tracking-tight tabular-nums">{Math.round(row.item.matchScore)}</div>
                  </div>
                  <div className="text-right text-sm text-ink-600">
                    <div>
                      Commute <b className="tabular-nums text-ink-900">{Math.round(row.item.commuteScore)}</b> × 50%
                    </div>
                    <div>
                      Livability <b className="tabular-nums text-ink-900">{Math.round(row.item.livability.index)}</b> × 50%
                    </div>
                  </div>
                </div>
                <ScoreStack parts={row.parts} height={14} className="mt-4" />
                <div className="mt-1 flex text-[11px] text-ink-500">
                  <span className="w-1/2">Commute</span>
                  <span className="w-1/2 pl-1">Livability</span>
                </div>
              </>
            ) : (
              <>
                <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Livability Index</div>
                <div className="text-5xl font-bold leading-none tracking-tight tabular-nums">{Math.round(detail.livability.index)}</div>
              </>
            )}
            <div className={`mt-5 rounded-xl px-4 py-3 ring-1 ring-inset ${toneCls}`}>
              <div className="flex items-center justify-between gap-2">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Last sold · {monthLabel(f.priceMonth)}</div>
                  <div className="text-2xl font-bold tracking-tight">{money(f.price)}</div>
                </div>
                <ValuationBadge label={v.label} />
              </div>
              <div className="mt-0.5 text-xs text-ink-600">
                {money(Math.round(f.price / f.floorAreaSqm))}/m² · Not part of the match score
              </div>
            </div>
          </div>
        </div>
      </Card>

      <div className="grid gap-5 lg:grid-cols-3">
        <Card className="p-5 lg:col-span-2">
          <SectionTitle
            right={
              <span className="flex items-center gap-1 text-xs text-ink-500">
                Uses your priorities <InfoTip text="Each part is scored 0-100. Commute is worth 50 points split between workplaces; livability is worth 50 points split by your weights." />
              </span>
            }
          >
            {row ? `Where the ${Math.round(row.item.matchScore)} points come from` : 'Livability breakdown'}
          </SectionTitle>
          {row ? (
            <div className="grid gap-x-8 md:grid-cols-2">
              <div>
                <h3 className="text-[11px] font-semibold uppercase tracking-wide text-ink-500">
                  Commute · {row.parts.filter((p) => p.kind === 'commute').reduce((s, p) => s + p.points, 0).toFixed(1)} of 50
                </h3>
                <ul className="divide-y divide-ink-100">
                  {row.parts
                    .filter((p) => p.kind === 'commute')
                    .map((p) => (
                      <PointsRow key={p.key} part={p} detail={legDetail(p)} delta={deltaOf(p)} />
                    ))}
                </ul>
              </div>
              <div>
                <h3 className="mt-3 text-[11px] font-semibold uppercase tracking-wide text-ink-500 md:mt-0">
                  Livability · {row.parts.filter((p) => p.kind === 'livability').reduce((s, p) => s + p.points, 0).toFixed(1)} of 50
                </h3>
                <ul className="divide-y divide-ink-100">
                  {row.parts
                    .filter((p) => p.kind === 'livability' && p.share > 0)
                    .map((p) => (
                      <PointsRow key={p.key} part={p} detail={factorDetail[p.key]} delta={deltaOf(p)} />
                    ))}
                </ul>
              </div>
            </div>
          ) : (
            <ul className="grid gap-x-8 divide-y divide-ink-100 sm:grid-cols-2 sm:divide-y-0">
              {(['lease', 'safety', 'density', 'airQuality'] as const).map((k) => {
                const part: Part = {
                  key: k,
                  kind: 'livability',
                  label: { lease: 'Remaining lease', safety: 'Safety', density: 'Space', airQuality: 'Air quality' }[k],
                  share: weights[k],
                  score: detail.livability[k],
                  points: (weights[k] * detail.livability[k]) / 100,
                  raw: '',
                };
                return <PointsRow key={k} part={part} detail={factorDetail[k]} />;
              })}
            </ul>
          )}
          {row && <p className="mt-3 text-xs text-ink-500">“vs avg” compares each part with the average of the {total} flats in your results.</p>}
        </Card>

        <div className="space-y-5">
          <Card className="p-5">
            <SectionTitle>Commute</SectionTitle>
            {detail.commute ? (
              <div className="space-y-3">
                {detail.commute.map((leg) => {
                  const Icon = MODE_ICON[leg.mode];
                  const a = detail.anchors[leg.anchorIndex];
                  return (
                    <div key={leg.anchorIndex} className="flex items-center gap-3">
                      <span
                        className="flex h-8 w-9 shrink-0 items-center justify-center rounded-lg text-xs font-bold text-white"
                        style={{ background: leg.anchorIndex === 0 ? PART_COLOR.w0 : PART_COLOR.w1 }}
                      >
                        W{leg.anchorIndex + 1}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-sm font-medium">{a?.label}</div>
                        <div className="flex items-center gap-1 text-xs text-ink-500">
                          <Icon className="h-3.5 w-3.5" /> {MODE_LABEL[leg.mode]}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-lg font-bold tabular-nums">{leg.minutes}</div>
                        <div className="-mt-1 text-[11px] text-ink-500">min</div>
                      </div>
                    </div>
                  );
                })}
                <p className="pt-1 text-xs text-ink-500">One way, door to door, fastest of your chosen modes.</p>
              </div>
            ) : (
              <p className="text-sm text-ink-500">Commute is not available because there is no search this session. Run a search to see travel times to your workplaces.</p>
            )}
          </Card>

          <Card className="p-5">
            <SectionTitle>
              <span className="flex items-center gap-2">
                <GraduationCap className="h-4 w-4 text-ink-400" /> Primary schools within 2 km
              </span>
            </SectionTitle>
            {detail.nearbySchools.length ? (
              <ul className="space-y-2 text-sm">
                {detail.nearbySchools.map((s) => (
                  <li key={s.name} className="flex justify-between gap-2">
                    <span className="text-ink-700">{s.name}</span>
                    <span className="shrink-0 tabular-nums text-ink-500">{s.distanceKm.toFixed(1)} km</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-ink-500">None in our sample data.</p>
            )}
            <p className="mt-3 text-[11px] text-ink-500">For information only. Not part of the score.</p>
          </Card>
        </div>

        <Card className="p-5 lg:col-span-2">
          <SectionTitle right={<span className="text-xs text-ink-500">Estimate, not a guarantee</span>}>
            <span className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-ink-400" /> Fair-value pricing
            </span>
          </SectionTitle>
          {v.fairValue != null && v.rangeLow != null && v.rangeHigh != null && v.price != null ? (
            <>
              <p className="text-sm text-ink-600">
                Similar {flatTypeLabel(f.flatType).toLowerCase()} flats in {titleCase(f.town)} sold for an average of <b>{money(Math.round(v.estateAveragePsm ?? 0))}/m²</b> over{' '}
                {monthLabel(v.windowStart)} to {monthLabel(v.windowEnd)} ({v.sampleSize} sales). For this flat's size that is a fair value of about <b>{money(v.fairValue)}</b>. {VALUATION[v.label].help}
              </p>
              <FairValueBar price={v.price} low={v.rangeLow} fair={v.fairValue} high={v.rangeHigh} />
              <div className="mt-4 flex flex-wrap gap-x-6 gap-y-1 text-xs text-ink-500">
                <span>
                  Fair range: {money(v.rangeLow)} to {money(v.rangeHigh)} (±{v.fairBandPercent}%)
                </span>
                <span>Estate average price: {money(v.estateAveragePrice)}</span>
                <span className="inline-flex items-center gap-1">
                  <CalendarClock className="h-3.5 w-3.5" /> Baseline updated {dateLabel(v.baselineDate)}
                </span>
              </div>
              {v.stale && v.message && <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-900">{v.message}</p>}
            </>
          ) : (
            <p className="text-sm text-ink-600">{v.message}</p>
          )}
        </Card>

        <Card className="p-5">
          <SectionTitle>Sale history</SectionTitle>
          <PriceHistory points={detail.transactions} />
        </Card>

        <Card className="overflow-hidden lg:col-span-3">
          <button
            onClick={() => setCpfOpen((o) => !o)}
            className="flex w-full items-center justify-between gap-3 p-5 text-left hover:bg-ink-50/60 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-600"
            aria-expanded={cpfOpen}
          >
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
                <Calculator className="h-5 w-5" />
              </span>
              <div>
                <div className="font-semibold">CPF grants &amp; affordability</div>
                <div className="text-sm text-ink-500">Can you afford {moneyShort(f.price)}? Estimate your CPF grants, loan and monthly instalment.</div>
              </div>
            </div>
            <ChevronDown className={`h-5 w-5 shrink-0 text-ink-400 transition ${cpfOpen ? 'rotate-180' : ''}`} />
          </button>
          {cpfOpen && (
            <div className="border-t border-ink-100 p-5">
              <CPFCalculatorPanel flatId={flatId} />
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
