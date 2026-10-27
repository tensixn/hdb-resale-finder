import { Check } from 'lucide-react';
import { activePreset, PRESETS } from '../lib/priorities';
import type { Weights } from '../types/models';

/** Preset chips above the sliders: one tap sets all four livability weights (UC-03). */
export function PriorityPresets({ weights, onPick, busy }: { weights: Weights; onPick: (w: Weights) => void; busy?: boolean }) {
  const active = activePreset(weights);
  return (
    <div role="group" aria-label="Quick priorities" className="flex flex-wrap gap-1.5">
      {PRESETS.map((p) => {
        const on = active?.id === p.id;
        const Icon = on ? Check : p.icon;
        return (
          <button
            key={p.id}
            type="button"
            aria-pressed={on}
            disabled={busy}
            title={p.blurb}
            onClick={() => !on && onPick(p.weights)}
            className={`inline-flex min-h-[36px] items-center gap-1.5 rounded-full px-3 text-sm font-medium ring-1 ring-inset transition focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 disabled:opacity-60 max-sm:min-h-[44px] ${
              on ? 'bg-ink-900 text-white ring-ink-900' : 'bg-white text-ink-700 ring-ink-200 hover:bg-ink-50'
            }`}
          >
            <Icon className="h-4 w-4" aria-hidden /> {p.label}
          </button>
        );
      })}
      {!active && <span className="inline-flex min-h-[36px] items-center px-1 text-xs font-medium text-ink-500 max-sm:min-h-[44px]">Custom mix</span>}
    </div>
  );
}
