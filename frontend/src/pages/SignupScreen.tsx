import { Eye, EyeOff, UserPlus } from '../components/icons';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, ApiError } from '../api/apiClient';
import { Button, Field, inputClass } from '../components/ui';
import { useAuth } from '../state/authStore';
import { finishPendingAction } from './authFlow';
import { AuthLayout } from './LoginScreen';

/** UC-13: register, accept the PDPA notice, and continue the original action. */
export default function SignupScreen() {
  const [params] = useSearchParams();
  const next = params.get('next') || '/profile';
  const navigate = useNavigate();
  const signIn = useAuth((s) => s.signIn);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [show, setShow] = useState(false);
  const [consent, setConsent] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrors({});
    setError(null);
    try {
      const r = await api.register(email, password, confirm, consent);
      signIn(r.token, r.email);
      await finishPendingAction();
      navigate(next, { replace: true });
    } catch (err) {
      const ae = err as ApiError;
      setErrors(ae.fieldErrors ?? {});
      setError(Object.keys(ae.fieldErrors ?? {}).length ? null : ae.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Create your account" subtitle="Free. Keep your bookmarks, priorities and financial profile across visits.">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Email" htmlFor="email" error={errors.email}>
          <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass(errors.email)} autoFocus />
        </Field>
        <Field label="Password" htmlFor="password" error={errors.password} hint="At least 8 characters with a letter and a number">
          <div className="relative">
            <input id="password" type={show ? 'text' : 'password'} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} className={`${inputClass(errors.password)} pr-11`} />
            <button type="button" onClick={() => setShow(!show)} aria-label={show ? 'Hide passwords' : 'Show passwords'} aria-pressed={show} className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-ink-500 hover:text-ink-900">
              {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </Field>
        <Field label="Confirm password" htmlFor="confirm" error={errors.confirmPassword}>
          <input id="confirm" type={show ? 'text' : 'password'} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} className={inputClass(errors.confirmPassword)} />
        </Field>
        <div>
          <label className="flex items-start gap-2.5 rounded-xl bg-ink-50 p-3 text-xs leading-relaxed text-ink-600">
            <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} className="mt-0.5 h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500" />
            <span>
              I agree that HomeMatch SG may store my email, saved flats and, if I choose to save it, my financial profile, to provide this service. Financial details are encrypted and are never shared with third parties. I can delete my financial profile at any time (PDPA).
            </span>
          </label>
          {errors.consent && <p className="mt-1.5 text-sm text-rose-600">{errors.consent}</p>}
        </div>
        {error && (
          <p className="rounded-xl bg-rose-50 px-3 py-2.5 text-sm text-rose-700" role="alert">
            {error}
            {error.includes('already registered') && (
              <>
                {' '}
                <Link to={`/login?next=${encodeURIComponent(next)}`} className="font-semibold underline">
                  Sign in
                </Link>
              </>
            )}
          </p>
        )}
        <Button type="submit" loading={loading} className="w-full" size="lg">
          <UserPlus className="h-4 w-4" /> Create account
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-ink-600">
        Already have an account?{' '}
        <Link to={`/login?next=${encodeURIComponent(next)}`} className="font-semibold text-brand-700 hover:underline">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
