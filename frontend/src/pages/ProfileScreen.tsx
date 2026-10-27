import { LogOut, Pencil, Search, ShieldCheck, Trash2, UserRound, Wallet } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, ApiError } from '../api/apiClient';
import { LivabilitySliders } from '../components/LivabilitySliders';
import { Button, Card, Field, inputClass, SectionTitle, Spinner } from '../components/ui';
import { dateLabel, money, titleCase } from '../lib/format';
import { useAuth } from '../state/authStore';
import { useBookmarks } from '../state/bookmarkStore';
import { useSearch } from '../state/searchStore';
import { toast } from '../state/toastStore';
import type { FinancialProfile, MaritalStatus, Profile } from '../types/models';

const STATUS_LABEL: Record<MaritalStatus, string> = { MARRIED: 'Married', ENGAGED: 'Engaged', SINGLE: 'Single' };

/** UC-14 Manage Profile and UC-15 Sign Out. */
export default function ProfileScreen() {
  const navigate = useNavigate();
  const { signOut } = useAuth();
  const resetBookmarks = useBookmarks((s) => s.reset);
  const search = useSearch();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<FinancialProfile>({ householdIncome: null, maritalStatus: null, parentPostalCode: '', livingWithParents: false });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const load = () => api.profile().then(setProfile).catch((e: ApiError) => toast(e.message, 'error'));
  useEffect(() => {
    load();
  }, []);

  if (!profile) return <Spinner label="Loading your profile" />;

  const startEdit = () => {
    setDraft(profile.financial ? { ...profile.financial, parentPostalCode: profile.financial.parentPostalCode ?? '' } : { householdIncome: null, maritalStatus: null, parentPostalCode: '', livingWithParents: false });
    setErrors({});
    setEditing(true);
  };

  const saveFinancial = async () => {
    setSaving(true);
    try {
      const p = await api.saveFinancial(draft);
      setProfile(p);
      setEditing(false);
      toast('Financial profile saved', 'success');
    } catch (e) {
      const ae = e as ApiError;
      setErrors(ae.fieldErrors ?? {});
      if (!Object.keys(ae.fieldErrors ?? {}).length) toast(ae.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const deleteFinancial = async () => {
    if (!window.confirm('Delete your saved financial profile? This removes it from our database.')) return;
    await api.deleteFinancial();
    toast('Financial profile deleted', 'success');
    load();
  };

  const doSignOut = async () => {
    await signOut();
    resetBookmarks();
    search.clear();
    toast('Signed out', 'info');
    navigate('/', { replace: true });
  };

  return (
    <div className="mx-auto max-w-4xl px-4 pb-28 pt-6 md:pb-16 lg:px-6">
      <div className="mb-6 flex items-center gap-4">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-600 text-xl font-bold uppercase text-white">{profile.email[0]}</div>
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-xl font-bold tracking-tight">{profile.email}</h1>
          <p className="text-sm text-ink-500">Member since {dateLabel(profile.memberSince).split(',')[0]}</p>
        </div>
        <Button variant="secondary" onClick={doSignOut}>
          <LogOut className="h-4 w-4" /> Sign out
        </Button>
      </div>

      <div className="grid gap-5 md:grid-cols-2">
        <Card className="p-5">
          <SectionTitle>
            <span className="flex items-center gap-2">
              <Wallet className="h-4 w-4 text-ink-400" /> Financial profile
            </span>
          </SectionTitle>
          {editing ? (
            <div className="space-y-4">
              <Field label="Monthly household income" htmlFor="p-income" error={errors.householdIncome}>
                <input id="p-income" inputMode="numeric" value={draft.householdIncome ?? ''} onChange={(e) => setDraft({ ...draft, householdIncome: e.target.value === '' ? null : Number(e.target.value.replace(/[^\d]/g, '')) })} className={inputClass(errors.householdIncome)} />
              </Field>
              <Field label="Marital status" error={errors.maritalStatus}>
                <select value={draft.maritalStatus ?? ''} onChange={(e) => setDraft({ ...draft, maritalStatus: (e.target.value || null) as MaritalStatus | null })} className={inputClass(errors.maritalStatus)}>
                  <option value="">Choose…</option>
                  <option value="MARRIED">Married</option>
                  <option value="ENGAGED">Engaged</option>
                  <option value="SINGLE">Single (35+)</option>
                </select>
              </Field>
              <Field label="Parents' postal code (optional)" htmlFor="p-ppc" error={errors.parentPostalCode}>
                <input id="p-ppc" maxLength={6} inputMode="numeric" value={draft.parentPostalCode} onChange={(e) => setDraft({ ...draft, parentPostalCode: e.target.value.replace(/[^\d]/g, '') })} className={inputClass(errors.parentPostalCode)} />
              </Field>
              <label className="flex items-center gap-2 text-sm text-ink-700">
                <input type="checkbox" checked={draft.livingWithParents} onChange={(e) => setDraft({ ...draft, livingWithParents: e.target.checked })} className="h-4 w-4 rounded border-ink-300 text-brand-600" />
                Living with parents
              </label>
              <div className="flex justify-end gap-2">
                <Button variant="ghost" onClick={() => setEditing(false)}>
                  Cancel
                </Button>
                <Button onClick={saveFinancial} loading={saving}>
                  Save
                </Button>
              </div>
            </div>
          ) : profile.financial ? (
            <>
              <dl className="grid grid-cols-2 gap-y-2 text-sm">
                <dt className="text-ink-500">Monthly income</dt>
                <dd className="text-right font-medium">{money(profile.financial.householdIncome)}</dd>
                <dt className="text-ink-500">Marital status</dt>
                <dd className="text-right font-medium">{profile.financial.maritalStatus ? STATUS_LABEL[profile.financial.maritalStatus] : '–'}</dd>
                <dt className="text-ink-500">Parents</dt>
                <dd className="text-right font-medium">{profile.financial.livingWithParents ? 'Living together' : profile.financial.parentPostalCode ? `Postal ${profile.financial.parentPostalCode}` : 'Not given'}</dd>
              </dl>
              <p className="mt-3 flex items-center gap-1.5 text-xs text-ink-500">
                <ShieldCheck className="h-3.5 w-3.5 text-brand-600" /> Encrypted at rest. Updated {dateLabel(profile.financialUpdatedAt)}.
              </p>
              <div className="mt-4 flex gap-2">
                <Button variant="secondary" size="sm" onClick={startEdit}>
                  <Pencil className="h-3.5 w-3.5" /> Edit
                </Button>
                <Button variant="danger" size="sm" onClick={deleteFinancial}>
                  <Trash2 className="h-3.5 w-3.5" /> Delete
                </Button>
              </div>
            </>
          ) : (
            <div className="text-sm text-ink-500">
              <p>No financial profile saved. Saving one pre-fills the CPF grant calculator.</p>
              <Button variant="secondary" size="sm" className="mt-3" onClick={startEdit}>
                Add financial profile
              </Button>
            </div>
          )}
        </Card>

        <Card className="p-5">
          <SectionTitle>
            <span className="flex items-center gap-2">
              <UserRound className="h-4 w-4 text-ink-400" /> Livability priorities
            </span>
          </SectionTitle>
          <LivabilitySliders
            weights={profile.weights}
            onCommit={async (f, v) => {
              await search.adjustWeight(f, v);
              load();
            }}
            onReset={async () => {
              await search.resetWeights();
              load();
              toast('Priorities reset to equal weights', 'success');
            }}
            compact
          />
        </Card>

        <Card className="p-5 md:col-span-2">
          <SectionTitle>
            <span className="flex items-center gap-2">
              <Search className="h-4 w-4 text-ink-400" /> Last search
            </span>
          </SectionTitle>
          {profile.lastSearch ? (
            <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
              <div className="text-ink-700">
                <b>{profile.lastSearch.workplace1}</b>
                {profile.lastSearch.workplace2 && (
                  <>
                    {' '}
                    and <b>{profile.lastSearch.workplace2}</b>
                  </>
                )}
                {profile.lastSearch.maxPrice && <> · up to {money(profile.lastSearch.maxPrice)}</>}
                {profile.lastSearch.towns && <> · {profile.lastSearch.towns.split(',').map(titleCase).join(', ')}</>}
                <div className="text-xs text-ink-500">{dateLabel(profile.lastSearch.at)}</div>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  const l = profile.lastSearch!;
                  search.setForm({
                    workplace1: l.workplace1,
                    workplace2: l.workplace2 ?? '',
                    transportModes: l.transportModes.split(',') as never,
                    minPrice: l.minPrice?.toString() ?? '',
                    maxPrice: l.maxPrice?.toString() ?? '',
                    flatTypes: l.flatTypes ? l.flatTypes.split(',') : [],
                    towns: l.towns ? l.towns.split(',') : [],
                  });
                  navigate('/');
                }}
              >
                Use again
              </Button>
            </div>
          ) : (
            <p className="text-sm text-ink-500">Your searches will show here.</p>
          )}
        </Card>
      </div>
    </div>
  );
}
