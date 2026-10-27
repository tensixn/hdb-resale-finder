import { LogIn } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, ApiError } from '../api/apiClient';
import { Logo } from '../components/NavBar';
import { Button, Card, Field, inputClass } from '../components/ui';
import { useAuth } from '../state/authStore';
import { finishPendingAction } from './authFlow';

/** UC-11: sign in, then return to the screen that asked for it and finish the original action. */
export default function LoginScreen() {
  const [params] = useSearchParams();
  const next = params.get('next') || '/profile';
  const navigate = useNavigate();
  const signIn = useAuth((s) => s.signIn);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setErrors({});
    try {
      const r = await api.login(email, password);
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
    <AuthLayout title="Welcome back" subtitle="Sign in to save flats, compare them and keep your preferences.">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Email" htmlFor="email" error={errors.email}>
          <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass(errors.email)} autoFocus />
        </Field>
        <Field label="Password" htmlFor="password" error={errors.password}>
          <input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} className={inputClass(errors.password)} />
        </Field>
        {error && (
          <p className="rounded-xl bg-rose-50 px-3 py-2.5 text-sm text-rose-700" role="alert">
            {error}
          </p>
        )}
        <Button type="submit" loading={loading} className="w-full" size="lg">
          <LogIn className="h-4 w-4" /> Sign in
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-ink-600">
        New here?{' '}
        <Link to={`/signup?next=${encodeURIComponent(next)}`} className="font-semibold text-brand-700 hover:underline">
          Create an account
        </Link>
      </p>
      <p className="mt-2 text-center">
        <button onClick={() => navigate(-1)} className="text-sm text-ink-500 hover:text-ink-800">
          Cancel
        </button>
      </p>
    </AuthLayout>
  );
}

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <div className="flex min-h-[calc(100dvh-64px)] items-start justify-center px-4 pb-24 pt-10 md:items-center md:pt-0">
      <Card className="w-full max-w-md p-7">
        <div className="mb-6">
          <div className="mb-6 md:hidden">
            <Logo />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          <p className="mt-1 text-sm text-ink-500">{subtitle}</p>
        </div>
        {children}
      </Card>
    </div>
  );
}
