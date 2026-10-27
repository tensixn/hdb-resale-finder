import { CheckCircle2, Info, XCircle } from 'lucide-react';
import { useToasts } from '../state/toastStore';

export function Toasts() {
  const { toasts, dismiss } = useToasts();
  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-20 z-[60] flex flex-col items-center gap-2 px-4 md:bottom-6">
      {toasts.map((t) => (
        <button
          key={t.id}
          onClick={() => dismiss(t.id)}
          className="pointer-events-auto flex items-center gap-2 rounded-xl bg-ink-900 px-4 py-2.5 text-sm font-medium text-white shadow-pop"
        >
          {t.tone === 'success' ? (
            <CheckCircle2 className="h-4 w-4 text-emerald-400" />
          ) : t.tone === 'error' ? (
            <XCircle className="h-4 w-4 text-rose-400" />
          ) : (
            <Info className="h-4 w-4 text-sky-300" />
          )}
          {t.message}
        </button>
      ))}
    </div>
  );
}
