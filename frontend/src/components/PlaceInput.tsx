import { MapPin } from './icons';
import { useEffect, useRef, useState } from 'react';
import { api } from '../api/apiClient';
import { inputClass } from './ui';

/** Workplace address field with suggestions for well-known places (offline gazetteer or OneMap). */
export function PlaceInput({
  id,
  value,
  onChange,
  placeholder,
  error,
  autoFocus,
}: {
  id: string;
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
  error?: string;
  autoFocus?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<string[]>([]);
  const [active, setActive] = useState(-1);
  const timer = useRef<number>();
  // The page focuses this field on load; only show suggestions once the user interacts with it.
  const skipFocus = useRef(!!autoFocus);

  useEffect(() => {
    if (!open) return;
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => {
      api.places(value).then(setItems).catch(() => setItems([]));
    }, 120);
    return () => window.clearTimeout(timer.current);
  }, [value, open]);

  const pick = (v: string) => {
    onChange(v);
    setOpen(false);
  };

  return (
    <div className="relative">
      <MapPin className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
      <input
        id={id}
        value={value}
        autoFocus={autoFocus}
        autoComplete="off"
        placeholder={placeholder}
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
          setActive(-1);
        }}
        onFocus={() => {
          if (skipFocus.current) skipFocus.current = false;
          else setOpen(true);
        }}
        onMouseDown={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        onKeyDown={(e) => {
          if (!open && e.key === 'ArrowDown') {
            setOpen(true);
            return;
          }
          if (!open || items.length === 0) return;
          if (e.key === 'ArrowDown') {
            e.preventDefault();
            setActive((a) => Math.min(items.length - 1, a + 1));
          } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setActive((a) => Math.max(0, a - 1));
          } else if (e.key === 'Enter' && active >= 0) {
            e.preventDefault();
            pick(items[active]);
          } else if (e.key === 'Escape') setOpen(false);
        }}
        className={`${inputClass(error)} pl-9`}
        aria-invalid={!!error}
      />
      {open && items.length > 0 && (
        <ul className="absolute z-30 mt-1 max-h-64 w-full overflow-auto rounded-xl bg-white py-1 shadow-pop ring-1 ring-ink-900/10">
          {items.map((it, i) => (
            <li key={it}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => pick(it)}
                className={`flex min-h-[44px] w-full items-center gap-2 px-3 py-2 text-left text-sm sm:min-h-0 ${i === active ? 'bg-brand-50 text-brand-800' : 'text-ink-700 hover:bg-ink-50'}`}
              >
                <MapPin className="h-3.5 w-3.5 text-ink-400" /> {it}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
