import { AlertTriangle, X } from './icons';
import { useState } from 'react';

/** Non-blocking notice shown when cached data is in use or a provider reports a problem (NFR-3). */
export function WarningBanner({ warnings }: { warnings: string[] }) {
  const [hidden, setHidden] = useState<string>('');
  const key = warnings.join('|');
  if (warnings.length === 0 || hidden === key) return null;
  return (
    <div className="flex items-start gap-3 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-900 ring-1 ring-inset ring-amber-200" role="status">
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
      <ul className="flex-1 space-y-0.5">
        {warnings.map((w) => (
          <li key={w}>{w}</li>
        ))}
      </ul>
      <button onClick={() => setHidden(key)} aria-label="Dismiss" className="-my-3 -mr-3 flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-amber-700 hover:text-amber-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-amber-600">
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
