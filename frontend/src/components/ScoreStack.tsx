import { PART_COLOR, type Part, type PartKey } from '../lib/explain';

/**
 * The match score drawn as its parts. Each part gets a slot as wide as the points it can give (its share of
 * 100), filled as far as it scored. Slots line up across every card, so scanning down the list compares like
 * with like: the first half is always commute, the second half is livability split by your weights.
 */
export function ScoreStack({
  parts,
  height = 10,
  highlight,
  className = '',
}: {
  parts: Part[];
  height?: number;
  highlight?: PartKey | null;
  className?: string;
}) {
  const total = parts.reduce((s, p) => s + p.points, 0);
  const label = `Match ${Math.round(total)} of 100: ${parts
    .filter((p) => p.share > 0)
    .map((p) => `${p.label} ${p.points.toFixed(1)} of ${p.share.toFixed(1)}`)
    .join(', ')}`;
  return (
    <div className={`flex w-full gap-[2px] ${className}`} role="img" aria-label={label}>
      {parts
        .filter((p) => p.share > 0)
        .map((p) => (
          <div
            key={p.key}
            className="relative overflow-hidden rounded-[3px] transition-[flex-grow] duration-500"
            style={{
              flexGrow: p.share,
              flexBasis: 0,
              height,
              background: `${PART_COLOR[p.key]}24`,
              outline: highlight === p.key ? `2px solid ${PART_COLOR[p.key]}` : undefined,
              outlineOffset: 1,
            }}
            title={`${p.label}: ${p.raw}, ${p.points.toFixed(1)} of ${p.share.toFixed(1)} pts`}
          >
            <div className="absolute inset-y-0 left-0 transition-[width] duration-500" style={{ width: `${p.score}%`, background: PART_COLOR[p.key] }} />
          </div>
        ))}
    </div>
  );
}

/** Labels that sit above a ScoreStack: "Commute 77" over the first half, "Livability 72" over the second. */
export function HalvesLabel({ commute, livability, className = '' }: { commute: number; livability: number; className?: string }) {
  return (
    <div className={`flex text-[11px] font-medium text-ink-500 ${className}`}>
      <span className="w-1/2 pr-1">
        Commute <b className="tabular-nums text-ink-800">{Math.round(commute)}</b>
      </span>
      <span className="w-1/2 pl-1">
        Livability <b className="tabular-nums text-ink-800">{Math.round(livability)}</b>
      </span>
    </div>
  );
}

export function Swatch({ k }: { k: PartKey }) {
  return <span className="inline-block h-2.5 w-2.5 shrink-0 rounded-sm" style={{ background: PART_COLOR[k] }} aria-hidden />;
}
