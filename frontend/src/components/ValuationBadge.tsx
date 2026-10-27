import type { ValuationLabel } from '../types/models';
import { VALUATION } from '../lib/format';

export function ValuationBadge({ label, size = 'md' }: { label: ValuationLabel; size?: 'sm' | 'md' }) {
  const v = VALUATION[label];
  return (
    <span
      title={v.help}
      className={`inline-flex items-center gap-1.5 rounded-full font-medium ring-1 ring-inset ${v.tone} ${
        size === 'sm' ? 'px-2 py-0.5 text-[11px]' : 'px-2.5 py-1 text-xs'
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${v.dot}`} />
      {v.label}
    </span>
  );
}
