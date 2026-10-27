import { Database, RefreshCw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/apiClient';
import { Button, Card, SectionTitle } from '../components/ui';
import { dateLabel, monthLabel } from '../lib/format';
import { toast } from '../state/toastStore';
import type { SystemStatus } from '../types/models';

const FAQ: { q: string; a: string }[] = [
  {
    q: 'Where do the flats and prices come from?',
    a: 'From HDB resale transactions published on data.gov.sg. There is no public API for live listings, so each flat is a unit that has been resold, and its price is the most recent transaction price.',
  },
  {
    q: 'How is the match score worked out?',
    a: 'It is the average of the commute score and the Livability Index, both from 0 to 100. A commute of 15 minutes or less scores 100 and 90 minutes or more scores 0. With two workplaces, the two commute scores are averaged.',
  },
  {
    q: 'What goes into the Livability Index?',
    a: "Four factors, each scored 0 to 100: years left on the lease, safety (reported crime per 1,000 residents in the town), space (residents per hectare), and air quality (the 24-hour PSI in the flat's region). Your priority sliders set how much each one counts.",
  },
  {
    q: 'What do Under-valued, Fair-valued and Over-valued mean?',
    a: "We take the average price per square metre of the same flat type in the same town over the last 6 months, and multiply it by this flat's floor area. Within 5% of that is fair. More than 5% below is under-valued, more than 5% above is over-valued. It is an estimate, not a valuation.",
  },
  {
    q: 'How often is pricing data updated?',
    a: 'Every 24 hours, an automatic job pulls new transactions from data.gov.sg and recalculates the 6-month averages. If that fails, the previous averages are kept and the date is shown next to the estimate.',
  },
  {
    q: 'Are the CPF grant amounts final?',
    a: 'No. They are estimates based on the published CPF Housing Grant, EHG and PHG rules at the time of release. HDB decides the final amounts, and EHG also needs 12 months of continuous employment, which we do not check.',
  },
  {
    q: 'What happens to my financial details?',
    a: 'As a guest, they stay in your browser tab and are never stored. If you sign in and choose to save them, they are encrypted in our database. They are never written to logs or sent to outside services, and you can delete them from your profile at any time.',
  },
];

/** In-app FAQ and data freshness (SRS 2.6). */
export default function HelpScreen() {
  const [status, setStatus] = useState<SystemStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const load = () => api.status().then(setStatus).catch(() => undefined);
  useEffect(() => {
    load();
  }, []);

  const refresh = async () => {
    setBusy(true);
    try {
      const r = await api.refreshValuation();
      toast(r.status === 'SUCCESS' ? `Refreshed: ${r.recordsIngested} new transactions` : `Refresh failed: ${r.message}`, r.status === 'SUCCESS' ? 'success' : 'error');
      load();
    } catch (e) {
      toast((e as ApiError).message, 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-4 pb-28 pt-6 md:pb-16 lg:px-6">
      <h1 className="mb-1 text-2xl font-bold tracking-tight">Help &amp; FAQ</h1>
      <p className="mb-6 text-sm text-ink-500">How HomeMatch SG works, and where its numbers come from.</p>

      {status && (
        <Card className="mb-6 p-5">
          <SectionTitle
            right={
              status.manualRefreshAllowed && (
                <Button variant="secondary" size="sm" onClick={refresh} loading={busy}>
                  <RefreshCw className="h-3.5 w-3.5" /> Refresh now
                </Button>
              )
            }
          >
            <span className="flex items-center gap-2">
              <Database className="h-4 w-4 text-ink-400" /> Data status
            </span>
          </SectionTitle>
          <dl className="grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
            <div className="flex justify-between">
              <dt className="text-ink-500">Flats</dt>
              <dd className="font-medium">{status.flats.toLocaleString()}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Transactions</dt>
              <dd className="font-medium">{status.transactions.toLocaleString()}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Latest sales month</dt>
              <dd className="font-medium">{monthLabel(status.latestTransactionMonth)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Pricing baseline</dt>
              <dd className="font-medium">{dateLabel(status.baselineComputedAt)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Last refresh</dt>
              <dd className="font-medium">
                {status.lastRefreshStatus ?? '–'} {status.lastRefreshRecords != null && `(${status.lastRefreshRecords} new)`}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-500">Data sources</dt>
              <dd className="font-medium">{status.dataMode === 'live' ? 'Live APIs' : 'Offline sample data'}</dd>
            </div>
          </dl>
          {status.dataMode === 'offline' && (
            <p className="mt-3 rounded-lg bg-ink-50 px-3 py-2 text-xs text-ink-600">
              This copy runs on bundled sample data and simulated travel times, so it works without API keys. Prices, crime and density figures are realistic samples, not official records.
            </p>
          )}
        </Card>
      )}

      <div className="space-y-3">
        {FAQ.map((f) => (
          <details key={f.q} className="group rounded-2xl bg-white p-5 shadow-card ring-1 ring-ink-900/5">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-3 font-semibold text-ink-900">
              {f.q}
              <span className="text-ink-400 transition group-open:rotate-45">+</span>
            </summary>
            <p className="mt-3 text-sm leading-relaxed text-ink-600">{f.a}</p>
          </details>
        ))}
      </div>

      <p className="mt-8 text-center text-xs text-ink-400">
        Map data © OneMap, Singapore Land Authority. Resale data from data.gov.sg under the Singapore Open Data Licence. All figures are estimates for reference only. Please confirm with HDB and CPF Board before deciding.
      </p>
    </div>
  );
}
