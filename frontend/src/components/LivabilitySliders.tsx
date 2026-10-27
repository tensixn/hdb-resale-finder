import { RotateCcw } from './icons';
import { useEffect, useState } from 'react';
import { FACTORS } from '../lib/format';
import type { LivabilityFactor, Weights } from '../types/models';
import { InfoTip } from './ui';

const COLORS: Record<keyof Weights, string> = {
  lease: '#1d7a6b',
  safety: '#6b7f3a',
  density: '#d98a2b',
  airQuality: '#3aa0c8',
};

/**
 * The four livability weight sliders (UC-03). While dragging, only the moved slider changes; when it is
 * released the server rebalances the other three so the total stays at 100% (FR-6).
 */
export function LivabilitySliders({
  weights,
  onCommit,
  onReset,
  compact,
  busy,
}: {
  weights: Weights;
  onCommit: (factor: LivabilityFactor, value: number) => void;
  onReset: () => void;
  compact?: boolean;
  busy?: boolean;
}) {
  const [draft, setDraft] = useState<Weights>(weights);
  useEffect(() => setDraft(weights), [weights]);

  const isDefault = Object.values(weights).every((v) => v === 25);

  return (
    <div className={busy ? 'opacity-80' : ''}>
      <div className="mb-4 flex h-2.5 overflow-hidden rounded-full bg-ink-100" aria-hidden>
        {FACTORS.map((f) => (
          <div key={f.key} className="h-full transition-all duration-300" style={{ width: `${draft[f.key]}%`, background: COLORS[f.key] }} />
        ))}
      </div>
      <div className={compact ? 'space-y-3' : 'space-y-4'}>
        {FACTORS.map((f) => (
          <div key={f.key}>
            <div className="mb-1 flex items-center justify-between">
              <label htmlFor={`w-${f.key}`} className="flex items-center gap-1.5 text-sm font-medium text-ink-800">
                <span className="h-2.5 w-2.5 rounded-sm" style={{ background: COLORS[f.key] }} />
                {f.label}
                <InfoTip text={f.help} />
              </label>
              <span className="w-12 text-right text-sm font-semibold tabular-nums text-ink-900">{Math.round(draft[f.key])}%</span>
            </div>
            <input
              id={`w-${f.key}`}
              type="range"
              min={0}
              max={100}
              step={1}
              value={draft[f.key]}
              onChange={(e) => setDraft({ ...draft, [f.key]: Number(e.target.value) })}
              onPointerUp={(e) => onCommit(f.factor as LivabilityFactor, Number((e.target as HTMLInputElement).value))}
              onKeyUp={(e) => onCommit(f.factor as LivabilityFactor, Number((e.target as HTMLInputElement).value))}
              className="range w-full"
              style={{ ['--fill' as string]: COLORS[f.key], ['--pct' as string]: `${draft[f.key]}%` }}
            />
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-ink-500">
        <span>Weights always add up to 100%.</span>
        {!isDefault && (
          <button onClick={onReset} className="inline-flex items-center gap-1 font-medium text-brand-700 hover:text-brand-800">
            <RotateCcw className="h-3 w-3" /> Reset to equal
          </button>
        )}
      </div>
    </div>
  );
}
