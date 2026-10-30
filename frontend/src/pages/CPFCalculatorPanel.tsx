import { Calculator, ShieldCheck } from '../components/icons';
import { useEffect, useRef, useState } from 'react';
import { api, ApiError } from '../api/apiClient';
import { Button, Field, InfoTip, inputClass } from '../components/ui';
import { money } from '../lib/format';
import { useAuth } from '../state/authStore';
import type { CpfResult, FinancialProfile, MaritalStatus } from '../types/models';

const GRANT_NAMES: Record<string, string> = {
  CHG: 'CPF Housing Grant (Family / Singles)',
  EHG: 'Enhanced CPF Housing Grant',
  PHG: 'Proximity Housing Grant',
};

const GUEST_KEY = 'hm.cpf';

const RATING: Record<CpfResult['affordability']['rating'], { label: string; tone: string; bar: string }> = {
  COMFORTABLE: { label: 'Comfortable', tone: 'text-emerald-700 bg-emerald-50', bar: 'bg-emerald-500' },
  STRETCHED: { label: 'Stretched', tone: 'text-amber-800 bg-amber-50', bar: 'bg-amber-500' },
  ABOVE_LIMIT: { label: 'Above limit', tone: 'text-rose-700 bg-rose-50', bar: 'bg-rose-500' },
  UNKNOWN: { label: 'Unknown', tone: 'text-ink-600 bg-ink-100', bar: 'bg-ink-400' },
};

/**
 * UC-06 (CPF grants and affordability) with UC-07 (financial profile) inline. A guest's profile is kept only
 * for this browser session (UC-07.AC.2); a signed-in user can tick "Save for next time".
 */
export function CPFCalculatorPanel({ flatId }: { flatId: number }) {
  const email = useAuth((s) => s.email);
  const [profile, setProfile] = useState<FinancialProfile>({ householdIncome: null, maritalStatus: null, parentPostalCode: '', livingWithParents: false });
  const [save, setSave] = useState(false);
  const [tenure, setTenure] = useState(25);
  const [down, setDown] = useState(25);
  const [rate, setRate] = useState(2.6);
  const [result, setResult] = useState<CpfResult | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [prefilled, setPrefilled] = useState(false);
  const first = useRef(true);

  // UC-07.AC.1: pre-fill from the saved profile, or from this session for guests.
  useEffect(() => {
    if (email) {
      api.savedFinancialProfile().then((p) => {
        if (p) {
          setProfile({ ...p, parentPostalCode: p.parentPostalCode ?? '' });
          setPrefilled(true);
        }
      }).catch(() => undefined);
    } else {
      try {
        const raw = sessionStorage.getItem(GUEST_KEY);
        if (raw) setProfile(JSON.parse(raw));
      } catch {
        /* ignore */
      }
    }
  }, [email]);

  const complete = profile.householdIncome != null && profile.maritalStatus != null;

  const calculate = async (opts?: { quiet?: boolean }) => {
    const errs: Record<string, string> = {};
    if (profile.householdIncome == null) errs.householdIncome = 'Enter your monthly household income.';
    if (!profile.maritalStatus) errs.maritalStatus = 'Choose your marital status.';
    if (profile.parentPostalCode && !/^\d{6}$/.test(profile.parentPostalCode)) errs.parentPostalCode = 'Enter a 6-digit postal code.';
    setErrors(errs);
    if (Object.keys(errs).length) return;
    setLoading(!opts?.quiet);
    setError(null);
    try {
      const r = await api.estimateCpf({
        flatId,
        profile: { ...profile, parentPostalCode: profile.parentPostalCode || '' },
        saveProfile: save && !!email,
        loanTenureYears: tenure,
        downPaymentPercent: down,
        interestRatePercent: rate,
      });
      setResult(r);
      if (!email) {
        try {
          sessionStorage.setItem(GUEST_KEY, JSON.stringify(profile));
        } catch {
          /* ignore */
        }
      }
    } catch (e) {
      const err = e as ApiError;
      setErrors(err.fieldErrors ?? {});
      setError(Object.keys(err.fieldErrors ?? {}).length ? null : err.message);
    } finally {
      setLoading(false);
    }
  };

  // FR-13: changing loan assumptions recalculates straight away once there is a result.
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    if (result) {
      const t = setTimeout(() => calculate({ quiet: true }), 250);
      return () => clearTimeout(t);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenure, down, rate]);

  const statuses: { v: MaritalStatus; label: string }[] = [
    { v: 'MARRIED', label: 'Married' },
    { v: 'ENGAGED', label: 'Engaged' },
    { v: 'SINGLE', label: 'Single (35+)' },
  ];

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <div className="space-y-4">
        {prefilled && (
          <p className="flex items-center gap-2 rounded-xl bg-brand-50 px-3 py-2 text-xs text-brand-800">
            <ShieldCheck className="h-4 w-4" /> Filled in from your saved profile.
          </p>
        )}
        <Field label="Monthly household income" htmlFor="income" error={errors.householdIncome}>
          <div className="relative">
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm text-ink-400">S$</span>
            <input
              id="income"
              inputMode="numeric"
              value={profile.householdIncome ?? ''}
              onChange={(e) => {
                const v = e.target.value.replace(/[^\d]/g, '');
                setProfile({ ...profile, householdIncome: v === '' ? null : Number(v) });
              }}
              placeholder="e.g. 6500"
              className={`${inputClass(errors.householdIncome)} pl-10`}
            />
          </div>
        </Field>
        <Field label="Marital status" error={errors.maritalStatus}>
          <div className="grid grid-cols-3 gap-2">
            {statuses.map((s) => (
              <button
                key={s.v}
                type="button"
                onClick={() => setProfile({ ...profile, maritalStatus: s.v })}
                className={`rounded-xl px-2 py-2.5 text-sm font-medium ring-1 ring-inset ${profile.maritalStatus === s.v ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-ink-700 ring-ink-200 hover:bg-ink-50'}`}
              >
                {s.label}
              </button>
            ))}
          </div>
        </Field>
        <label className="flex items-center gap-2.5 text-sm text-ink-700">
          <input type="checkbox" className="h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500" checked={profile.livingWithParents} onChange={(e) => setProfile({ ...profile, livingWithParents: e.target.checked })} />
          I will live with my parents in this flat
        </label>
        {!profile.livingWithParents && (
          <Field
            label={
              <span className="flex items-center gap-1.5">
                Parents' postal code <span className="font-normal text-ink-400">(optional)</span>
                <InfoTip text="Used only to check the Proximity Housing Grant (within 4 km). It is matched against our own postal table and never sent to an outside service." />
              </span>
            }
            htmlFor="ppc"
            error={errors.parentPostalCode}
          >
            <input id="ppc" inputMode="numeric" maxLength={6} value={profile.parentPostalCode} onChange={(e) => setProfile({ ...profile, parentPostalCode: e.target.value.replace(/[^\d]/g, '') })} placeholder="e.g. 560123" className={inputClass(errors.parentPostalCode)} />
          </Field>
        )}
        {email ? (
          <label className="flex items-center gap-2.5 text-sm text-ink-700">
            <input type="checkbox" className="h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500" checked={save} onChange={(e) => setSave(e.target.checked)} />
            Save for next time <span className="text-xs text-ink-400">(stored encrypted)</span>
          </label>
        ) : (
          <p className="text-xs text-ink-500">As a guest, your details stay in this browser tab only and are never stored on our server.</p>
        )}
        {error && <p className="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        <Button onClick={() => calculate()} loading={loading} className="w-full">
          <Calculator className="h-4 w-4" /> {result ? 'Recalculate' : 'Estimate my grants'}
        </Button>
      </div>

      <div className={`rounded-2xl bg-ink-50 p-5 ring-1 ring-inset ring-ink-900/5 ${!result ? 'flex items-center justify-center' : ''}`}>
        {!result ? (
          <div className="py-8 text-center text-sm text-ink-500">
            <Calculator className="mx-auto mb-3 h-8 w-8 text-ink-300" />
            {complete ? 'Press "Estimate my grants" to see your numbers.' : 'Fill in your details to see grants and your monthly instalment.'}
          </div>
        ) : (
          <div className="space-y-5">
            <div>
              <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-500">Estimated grants</div>
              <div className="space-y-2">
                {result.grants.map((g) => (
                  <div key={g.scheme} className="flex items-start justify-between gap-3 rounded-xl bg-white px-3.5 py-2.5 ring-1 ring-ink-900/5">
                    <div>
                      <div className="text-sm font-semibold">{GRANT_NAMES[g.scheme]}</div>
                      <div className="text-xs text-ink-500">{g.reason}</div>
                    </div>
                    <div className={`text-sm font-bold tabular-nums ${g.amount > 0 ? 'text-emerald-700' : 'text-ink-400'}`}>{money(g.amount)}</div>
                  </div>
                ))}
              </div>
            </div>

            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <dt className="text-ink-500">Price</dt>
              <dd className="text-right font-medium tabular-nums">{money(result.price)}</dd>
              <dt className="text-ink-500">Less grants</dt>
              <dd className="text-right font-medium tabular-nums text-emerald-700">−{money(result.totalGrants)}</dd>
              <dt className="text-ink-500">Down payment ({result.loan.downPaymentPercent}%)</dt>
              <dd className="text-right font-medium tabular-nums">−{money(result.downPayment)}</dd>
              <dt className="border-t border-ink-200 pt-2 font-semibold text-ink-800">Loan</dt>
              <dd className="border-t border-ink-200 pt-2 text-right font-semibold tabular-nums">{money(result.loanAmount)}</dd>
            </dl>

            <div className="rounded-2xl bg-white p-4 ring-1 ring-ink-900/5">
              <div className="flex items-end justify-between">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-ink-500">Monthly instalment</div>
                  <div className="text-3xl font-bold tracking-tight">{money(result.monthlyInstalment)}</div>
                </div>
                <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${RATING[result.affordability.rating].tone}`}>{RATING[result.affordability.rating].label}</span>
              </div>
              {result.affordability.instalmentToIncomePercent != null && (
                <div className="mt-3">
                  <div className="relative h-2 rounded-full bg-ink-100">
                    <div className={`h-full rounded-full ${RATING[result.affordability.rating].bar}`} style={{ width: `${Math.min(100, (result.affordability.instalmentToIncomePercent / 45) * 100)}%` }} />
                    <div className="absolute -top-1 h-4 w-0.5 bg-ink-800" style={{ left: `${(30 / 45) * 100}%` }} title="30% MSR limit" />
                  </div>
                  <p className="mt-1.5 text-xs text-ink-500">
                    {result.affordability.instalmentToIncomePercent}% of income. {result.affordability.message}
                  </p>
                </div>
              )}
            </div>

            <div>
              <div className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-ink-500">
                Loan assumptions <InfoTip text={`${result.loan.lender}. HDB lends up to ${result.loan.maxLoanToValuePercent}% of the price. Change these to see how the instalment moves.`} />
              </div>
              <div className="space-y-3 text-sm">
                <Slider label="Loan tenure" value={tenure} min={5} max={25} step={1} unit=" yrs" onChange={setTenure} />
                <Slider label="Down payment" value={down} min={25} max={100} step={1} unit="%" onChange={setDown} />
                <Slider label="Interest rate" value={rate} min={0} max={10} step={0.1} unit="%" onChange={setRate} />
              </div>
            </div>

            <ul className="space-y-1 border-t border-ink-200 pt-3 text-[11px] leading-relaxed text-ink-500">
              {result.disclaimers.map((d) => (
                <li key={d}>• {d}</li>
              ))}
              {result.profileSaved && <li className="font-medium text-brand-700">• Your financial profile was saved (encrypted).</li>}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}

function Slider({ label, value, min, max, step, unit, onChange }: { label: string; value: number; min: number; max: number; step: number; unit: string; onChange: (v: number) => void }) {
  return (
    <label className="block">
      <div className="mb-1 flex justify-between">
        <span className="text-ink-700">{label}</span>
        <span className="font-semibold tabular-nums">
          {value}
          {unit}
        </span>
      </div>
      <input type="range" min={min} max={max} step={step} value={value} onChange={(e) => onChange(Number(e.target.value))} className="range w-full" style={{ ['--fill' as string]: '#1d7a6b', ['--pct' as string]: `${((value - min) / (max - min)) * 100}%` }} />
    </label>
  );
}
