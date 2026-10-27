import { Check, ChevronDown, X } from './icons';
import { useEffect, useRef, useState } from 'react';

/** Searchable multi-select used for towns. An empty selection means "any". */
export function MultiSelect({
  options,
  value,
  onChange,
  placeholder,
  format = (s) => s,
}: {
  options: string[];
  value: string[];
  onChange: (v: string[]) => void;
  placeholder: string;
  format?: (s: string) => string;
}) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const filtered = options.filter((o) => format(o).toLowerCase().includes(q.toLowerCase()));
  const toggle = (o: string) => onChange(value.includes(o) ? value.filter((v) => v !== o) : [...value, o]);

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex min-h-[44px] w-full items-center gap-1.5 rounded-xl bg-white px-3 py-1.5 text-left text-sm ring-1 ring-inset ring-ink-200 hover:ring-ink-300 focus:outline-none focus:ring-2 focus:ring-brand-500"
        aria-expanded={open}
      >
        <div className="flex flex-1 flex-wrap gap-1">
          {value.length === 0 && <span className="py-1 text-ink-400">{placeholder}</span>}
          {value.slice(0, 4).map((v) => (
            <span key={v} className="inline-flex items-center gap-1 rounded-lg bg-brand-50 px-2 py-1 text-xs font-medium text-brand-800">
              {format(v)}
              <X
                className="h-3 w-3 cursor-pointer"
                onClick={(e) => {
                  e.stopPropagation();
                  toggle(v);
                }}
              />
            </span>
          ))}
          {value.length > 4 && <span className="py-1 text-xs text-ink-500">+{value.length - 4} more</span>}
        </div>
        <ChevronDown className="h-4 w-4 text-ink-400" />
      </button>
      {open && (
        <div className="absolute z-30 mt-1 w-full rounded-xl bg-white shadow-pop ring-1 ring-ink-900/10">
          <div className="border-b border-ink-100 p-2">
            <input
              autoFocus
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Type to filter"
              className="w-full rounded-lg bg-ink-50 px-3 py-2 text-sm outline-none placeholder:text-ink-400"
            />
          </div>
          <ul className="max-h-60 overflow-auto py-1">
            {filtered.map((o) => (
              <li key={o}>
                <button type="button" onClick={() => toggle(o)} className="flex w-full items-center justify-between px-3 py-2 text-left text-sm text-ink-700 hover:bg-ink-50">
                  {format(o)}
                  {value.includes(o) && <Check className="h-4 w-4 text-brand-600" />}
                </button>
              </li>
            ))}
          </ul>
          {value.length > 0 && (
            <div className="border-t border-ink-100 p-2 text-right">
              <button type="button" onClick={() => onChange([])} className="text-xs font-medium text-ink-500 hover:text-ink-800">
                Clear selection
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
