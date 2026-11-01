import { Info, Loader2 } from './icons';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { useEffect, useId, useState } from 'react';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';

const VARIANTS: Record<Variant, string> = {
  primary:
    'bg-gradient-to-b from-brand-500 to-brand-700 text-white shadow-[0_1px_2px_rgba(10,38,34,0.25),inset_0_1px_0_rgba(255,255,255,0.18)] hover:from-brand-600 hover:to-brand-800 disabled:opacity-50',
  secondary: 'bg-white text-ink-800 ring-1 ring-inset ring-ink-200 hover:bg-ink-50 disabled:text-ink-400',
  ghost: 'text-ink-700 hover:bg-ink-100 disabled:text-ink-300',
  danger: 'bg-white text-rose-700 ring-1 ring-inset ring-rose-200 hover:bg-rose-50',
};

export function Button({
  variant = 'primary',
  size = 'md',
  loading,
  className = '',
  children,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; size?: 'sm' | 'md' | 'lg'; loading?: boolean }) {
  const sizes = { sm: 'h-11 sm:h-8 px-3 text-sm gap-1.5', md: 'h-11 sm:h-10 px-4 text-sm gap-2', lg: 'h-12 px-6 text-base gap-2' };
  return (
    <button
      {...rest}
      disabled={rest.disabled || loading}
      className={`inline-flex items-center justify-center rounded-xl font-semibold transition duration-200 active:scale-[0.96] focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed ${VARIANTS[variant]} ${sizes[size]} ${className}`}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  );
}

export function Card({ className = '', children }: { className?: string; children: ReactNode }) {
  return <div className={`rounded-2xl bg-white shadow-card ring-1 ring-ink-900/5 ${className}`}>{children}</div>;
}

export function SectionTitle({ children, right }: { children: ReactNode; right?: ReactNode }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-3">
      <h2 className="text-[15px] font-semibold text-ink-900">{children}</h2>
      {right}
    </div>
  );
}

export function Chip({
  active,
  onClick,
  children,
  icon,
}: {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`inline-flex h-11 items-center gap-1.5 rounded-full px-3.5 text-sm font-medium transition duration-200 active:scale-[0.96] focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2 sm:h-9 ${
        active ? 'bg-brand-600 text-white shadow-sm' : 'bg-white text-ink-700 ring-1 ring-inset ring-ink-200 hover:ring-ink-300'
      }`}
    >
      {icon}
      {children}
    </button>
  );
}

export function Field({
  label,
  error,
  hint,
  children,
  htmlFor,
}: {
  label: ReactNode;
  error?: string;
  hint?: ReactNode;
  children: ReactNode;
  htmlFor?: string;
}) {
  const msgId = htmlFor ? `${htmlFor}-msg` : undefined;
  // Fields wrap arbitrary inputs, so link the hint/error to the control by id after render.
  useEffect(() => {
    const el = htmlFor ? document.getElementById(htmlFor) : null;
    if (!el || !msgId) return;
    if (error || hint) el.setAttribute('aria-describedby', msgId);
    else el.removeAttribute('aria-describedby');
    if (error) el.setAttribute('aria-invalid', 'true');
    else el.removeAttribute('aria-invalid');
  }, [htmlFor, msgId, error, hint]);
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium text-ink-800">
        {label}
      </label>
      {children}
      {error ? (
        <p id={msgId} className="mt-1.5 text-sm text-rose-600" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p id={msgId} className="mt-1.5 text-xs text-ink-500">{hint}</p>
      ) : null}
    </div>
  );
}

export const inputClass = (error?: string) =>
  `block w-full rounded-xl border-0 bg-white px-3.5 py-3 sm:py-2.5 text-base sm:text-sm text-ink-900 ring-1 ring-inset placeholder:text-ink-400 focus:ring-2 focus:ring-inset ${
    error ? 'ring-rose-300 focus:ring-rose-500' : 'ring-ink-200 focus:ring-brand-500'
  }`;

/** A small (i) that shows an explanation on hover or tap (SRS 2.6 tooltips). */
export function InfoTip({ text }: { text: string }) {
  const [open, setOpen] = useState(false);
  const id = useId();
  // WCAG 1.4.13: the tip can be dismissed with Escape without moving focus or the pointer.
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);
  return (
    // Hover handlers sit on the wrapper so the pointer can move onto the tip without it closing.
    <span className="relative inline-flex" onMouseEnter={() => setOpen(true)} onMouseLeave={() => setOpen(false)}>
      <button
        type="button"
        aria-describedby={open ? id : undefined}
        aria-label="More information"
        aria-expanded={open}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onClick={() => setOpen((o) => !o)}
        className="-m-[15px] rounded-full p-[15px] text-ink-500 hover:text-ink-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 sm:-m-[5px] sm:p-[5px]"
      >
        <Info className="h-3.5 w-3.5" aria-hidden />
      </button>
      {open && (
        <span
          id={id}
          role="tooltip"
          className="absolute bottom-full left-1/2 z-50 mb-2 w-56 -translate-x-1/2 rounded-lg bg-ink-900 px-3 py-2 text-xs font-normal leading-relaxed text-white shadow-pop"
        >
          {text}
        </span>
      )}
    </span>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-10 text-sm text-ink-500">
      <Loader2 className="h-5 w-5 animate-spin" /> {label}
    </div>
  );
}

export function EmptyState({ icon, title, children, as: Heading = 'h2' }: { icon: ReactNode; title: string; children?: ReactNode; as?: 'h1' | 'h2' | 'h3' }) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center">
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">{icon}</div>
      <Heading className="text-base font-semibold text-ink-900">{title}</Heading>
      {children && <div className="mt-1.5 max-w-sm text-sm text-ink-500">{children}</div>}
    </div>
  );
}
