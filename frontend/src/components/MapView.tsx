import maplibregl, { type GeoJSONSource, type Map as MLMap } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { useEffect, useRef } from 'react';
import { PART_COLOR, pts } from '../lib/explain';
import { flatTypeLabel, moneyShort, scoreColor, titleCase, VALUATION } from '../lib/format';
import { SINGAPORE_OUTLINE } from '../lib/singapore';
import type { Anchor } from '../types/models';
import type { ExplainedRow } from './RankCard';

const ONEMAP_ATTRIBUTION =
  '<img onerror="this.remove()" alt="" src="https://www.onemap.gov.sg/web-assets/images/logo/om_logo.png" style="height:20px;width:20px;display:inline;vertical-align:middle"/>&nbsp;<a href="https://www.onemap.gov.sg/" target="_blank" rel="noopener">OneMap</a>&nbsp;&copy;&nbsp;contributors&nbsp;&#124;&nbsp;<a href="https://www.sla.gov.sg/" target="_blank" rel="noopener">Singapore Land Authority</a>';

const SG_BOUNDS: [[number, number], [number, number]] = [
  [103.55, 1.16],
  [104.1, 1.5],
];
const TOP_N = 10;

const esc = (s: string) => s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);

/** The little "why" card that the map shows for the hovered or selected flat. */
function popupHtml(row: ExplainedRow): string {
  const r = row.item;
  const f = r.flat;
  const v = VALUATION[r.valuation];
  const bar = row.parts
    .filter((p) => p.share > 0)
    .map(
      (p) =>
        `<span style="flex:${p.share} 0 0;height:7px;border-radius:2px;background:${PART_COLOR[p.key]}24;position:relative;overflow:hidden"><span style="position:absolute;top:0;bottom:0;left:0;width:${p.score}%;background:${PART_COLOR[p.key]}"></span></span>`,
    )
    .join('');
  const line = (sign: '+' | '−', text: string, d: number, color: string) =>
    `<div style="display:flex;gap:6px;align-items:baseline;margin-top:3px"><b style="color:${color};width:10px">${sign}</b><span>${esc(text)} <b style="color:${color};white-space:nowrap">${sign}${pts(d)}</b></span></div>`;
  return `
    <div style="font:12px Inter,system-ui;color:#2a2e38">
      <div style="display:flex;justify-content:space-between;gap:10px;align-items:baseline">
        <b style="font-size:15px;color:#1a6258">#${row.rank}</b>
        <span style="color:#68738c">Match <b style="font-size:15px;color:#171a21">${Math.round(r.matchScore)}</b></span>
      </div>
      <div style="font-weight:600;font-size:13px;margin-top:2px">${esc(f.block)} ${esc(titleCase(f.street))}</div>
      <div style="color:#68738c">${esc(titleCase(f.town))} · ${esc(flatTypeLabel(f.flatType))} · <b style="color:#171a21">${moneyShort(f.price)}</b> · ${v.label}</div>
      <div style="display:flex;justify-content:space-between;margin-top:8px;color:#68738c;font-size:11px"><span>Commute <b style="color:#2a2e38">${Math.round(r.commuteScore)}</b></span><span>Livability <b style="color:#2a2e38">${Math.round(r.livability.index)}</b></span></div>
      <div style="display:flex;gap:2px;margin-top:3px">${bar}</div>
      <div style="margin-top:6px;line-height:1.35">
        ${row.helps ? line('+', row.helps.text, row.helps.delta, '#047857') : ''}
        ${row.hurts ? line('−', row.hurts.text, row.hurts.delta, '#be123c') : ''}
      </div>
    </div>`;
}

/**
 * Map of results. Pins are coloured by Livability Index (FR-17); the top ten by match carry their rank number
 * so the order on the list can be read on the map too. Hover and selection are shared with the list.
 */
export function MapView({
  rows,
  anchors,
  hoveredId,
  selectedId,
  onHover,
  onSelect,
  onOpen,
  padBottom = 0,
}: {
  rows: ExplainedRow[];
  anchors: Anchor[];
  hoveredId: number | null;
  selectedId: number | null;
  onHover: (id: number | null) => void;
  onSelect: (id: number | null) => void;
  onOpen: (id: number) => void;
  padBottom?: number;
}) {
  const container = useRef<HTMLDivElement>(null);
  const map = useRef<MLMap | null>(null);
  const popup = useRef<maplibregl.Popup | null>(null);
  const anchorMarkers = useRef<maplibregl.Marker[]>([]);
  const rankMarkers = useRef<Map<number, { marker: maplibregl.Marker; el: HTMLElement }>>(new Map());
  const ready = useRef(false);
  const handlers = useRef({ onHover, onSelect, onOpen });
  handlers.current = { onHover, onSelect, onOpen };
  const fitTo = useRef<maplibregl.LngLatBounds | null>(null);
  const fitPad = useRef({ top: 80, left: 50, right: 60, bottom: 50 });
  const rowsRef = useRef(rows);
  rowsRef.current = rows;

  useEffect(() => {
    if (!container.current) return;
    const m = new maplibregl.Map({
      container: container.current,
      style: {
        version: 8,
        sources: {
          outline: { type: 'geojson', data: SINGAPORE_OUTLINE },
          onemap: {
            type: 'raster',
            tiles: ['https://www.onemap.gov.sg/maps/tiles/Default/{z}/{x}/{y}.png'],
            tileSize: 256,
            minzoom: 11,
            maxzoom: 19,
            attribution: ONEMAP_ATTRIBUTION,
            bounds: [103.596, 1.1443, 104.1, 1.4835],
          },
        },
        layers: [
          { id: 'water', type: 'background', paint: { 'background-color': '#dbe9f2' } },
          { id: 'land', type: 'fill', source: 'outline', paint: { 'fill-color': '#f4f2ec', 'fill-outline-color': '#cfd8df' } },
          { id: 'tiles', type: 'raster', source: 'onemap', paint: { 'raster-opacity': 0.9, 'raster-saturation': -0.6 } },
        ],
      },
      bounds: SG_BOUNDS,
      maxBounds: [
        [103.4, 1.05],
        [104.25, 1.6],
      ],
      minZoom: 10,
      attributionControl: false,
    });
    m.addControl(new maplibregl.AttributionControl({ compact: true }), 'bottom-right');
    if (window.matchMedia('(min-width: 768px)').matches) m.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
    m.on('error', () => {
      /* tile failures are expected offline; the outline layer keeps the map readable */
    });

    m.on('load', () => {
      m.addSource('flats', { type: 'geojson', data: { type: 'FeatureCollection', features: [] }, promoteId: 'id' });
      m.addLayer({
        id: 'flat-halo',
        type: 'circle',
        source: 'flats',
        paint: {
          'circle-radius': ['case', ['boolean', ['feature-state', 'active'], false], 16, 0],
          'circle-color': '#1d7a6b',
          'circle-opacity': 0.22,
        },
      });
      m.addLayer({
        id: 'flat-pins',
        type: 'circle',
        source: 'flats',
        paint: {
          'circle-radius': ['interpolate', ['linear'], ['zoom'], 10, 4, 13, 6.5, 16, 9],
          'circle-color': ['get', 'color'],
          'circle-stroke-color': ['case', ['boolean', ['feature-state', 'active'], false], '#0a2622', '#ffffff'],
          'circle-stroke-width': ['case', ['boolean', ['feature-state', 'active'], false], 3, 1.25],
        },
      });
      m.on('mouseenter', 'flat-pins', (e) => {
        m.getCanvas().style.cursor = 'pointer';
        const id = e.features?.[0]?.id;
        if (typeof id === 'number') handlers.current.onHover(id);
      });
      m.on('mouseleave', 'flat-pins', () => {
        m.getCanvas().style.cursor = '';
        handlers.current.onHover(null);
      });
      m.on('click', 'flat-pins', (e) => {
        const id = e.features?.[0]?.id;
        if (typeof id === 'number') handlers.current.onSelect(id);
      });
      m.on('click', (e) => {
        if (m.queryRenderedFeatures(e.point, { layers: ['flat-pins'] }).length === 0) handlers.current.onSelect(null);
      });
      ready.current = true;
      m.fire('data-ready');
    });
    map.current = m;
    let lastW = container.current.clientWidth;
    const ro = new ResizeObserver(() => {
      m.resize();
      const w = m.getContainer().clientWidth;
      // A map that was hidden (phone list view) fits its bounds again once it becomes visible.
      if (lastW === 0 && w > 0 && fitTo.current) m.fitBounds(fitTo.current, { padding: fitPad.current, maxZoom: 14, duration: 0 });
      lastW = w;
    });
    ro.observe(container.current);
    const markers = rankMarkers.current;
    return () => {
      ro.disconnect();
      ready.current = false;
      markers.clear();
      m.remove();
      map.current = null;
    };
  }, []);

  // Pins, plus numbered markers for the top of the ranking.
  useEffect(() => {
    const m = map.current;
    if (!m) return;
    const apply = () => {
      const src = m.getSource('flats') as GeoJSONSource | undefined;
      if (!src) return;
      src.setData({
        type: 'FeatureCollection',
        features: rows.map((r) => ({
          type: 'Feature',
          id: r.item.flat.id,
          properties: { id: r.item.flat.id, color: scoreColor(r.item.livability.index) },
          geometry: { type: 'Point', coordinates: [r.item.flat.longitude, r.item.flat.latitude] },
        })),
      });
    };
    if (ready.current) apply();
    else m.once('data-ready', apply);

    const top = [...rows].sort((a, b) => a.rank - b.rank).slice(0, TOP_N);
    const keep = new Set(top.map((r) => r.item.flat.id));
    rankMarkers.current.forEach((v, id) => {
      if (!keep.has(id)) {
        v.marker.remove();
        rankMarkers.current.delete(id);
      }
    });
    top.forEach((r) => {
      const id = r.item.flat.id;
      let entry = rankMarkers.current.get(id);
      if (!entry) {
        const el = document.createElement('div');
        el.className = 'rank-marker';
        el.addEventListener('mouseenter', () => handlers.current.onHover(id));
        el.addEventListener('mouseleave', () => handlers.current.onHover(null));
        el.addEventListener('click', (e) => {
          e.stopPropagation();
          handlers.current.onSelect(id);
        });
        const marker = new maplibregl.Marker({ element: el, anchor: 'bottom' }).setLngLat([r.item.flat.longitude, r.item.flat.latitude]).addTo(m);
        entry = { marker, el };
        rankMarkers.current.set(id, entry);
      }
      entry.el.innerHTML = `<span>${r.rank}</span>`;
      entry.el.title = `#${r.rank} ${r.item.flat.block} ${titleCase(r.item.flat.street)}`;
      entry.el.style.zIndex = String(100 - r.rank);
      entry.el.classList.toggle('is-first', r.rank === 1);
    });
  }, [rows]);

  // Workplace anchors, and fit the view to them and the best results the first time they arrive.
  const anchorKey = anchors.map((a) => `${a.latitude},${a.longitude}`).join('|');
  useEffect(() => {
    const m = map.current;
    if (!m) return;
    anchorMarkers.current.forEach((mk) => mk.remove());
    anchorMarkers.current = anchors.map((a) => {
      const el = document.createElement('div');
      el.className = 'anchor-marker';
      el.innerHTML = `<span style="background:${a.index === 0 ? PART_COLOR.w0 : PART_COLOR.w1}">W${a.index + 1}</span>`;
      el.title = `Workplace ${a.index + 1}: ${a.label}`;
      return new maplibregl.Marker({ element: el, anchor: 'bottom' }).setLngLat([a.longitude, a.latitude]).addTo(m);
    });
    const best = [...rowsRef.current].sort((a, b) => a.rank - b.rank).slice(0, 40);
    const coords = [...anchors.map((a) => [a.longitude, a.latitude]), ...best.map((r) => [r.item.flat.longitude, r.item.flat.latitude])];
    if (coords.length > 1) {
      const b = new maplibregl.LngLatBounds(coords[0] as [number, number], coords[0] as [number, number]);
      coords.forEach((p) => b.extend(p as [number, number]));
      fitTo.current = b;
      fitPad.current = { top: 80, left: 40, right: 40, bottom: 50 + padBottom };
      if (m.getContainer().clientWidth > 0) m.fitBounds(b, { padding: fitPad.current, maxZoom: 14, duration: 0 });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [anchorKey]);

  // Highlight the hovered and selected flat on both pins and rank markers.
  const prevActive = useRef<number[]>([]);
  useEffect(() => {
    const m = map.current;
    const active = [hoveredId, selectedId].filter((x): x is number => x !== null);
    rankMarkers.current.forEach((v, id) => v.el.classList.toggle('is-active', active.includes(id)));
    if (!m || !ready.current || !m.getSource('flats')) return;
    prevActive.current.forEach((id) => m.setFeatureState({ source: 'flats', id }, { active: false }));
    active.forEach((id) => m.setFeatureState({ source: 'flats', id }, { active: true }));
    prevActive.current = active;
  }, [hoveredId, selectedId, rows]);

  // The "why" popup follows hover, and falls back to the selected flat.
  const shownId = hoveredId ?? selectedId;
  useEffect(() => {
    popup.current?.remove();
    popup.current = null;
    const m = map.current;
    const row = rows.find((x) => x.item.flat.id === shownId);
    if (!m || !row || !window.matchMedia('(min-width: 768px)').matches) return;
    const el = document.createElement('div');
    el.innerHTML = popupHtml(row);
    const btn = document.createElement('button');
    btn.textContent = 'View details →';
    btn.className = 'popup-btn';
    btn.onclick = () => handlers.current.onOpen(row.item.flat.id);
    el.appendChild(btn);
    popup.current = new maplibregl.Popup({ offset: 14, closeButton: false, closeOnClick: false, maxWidth: '280px', focusAfterOpen: false })
      .setLngLat([row.item.flat.longitude, row.item.flat.latitude])
      .setDOMContent(el)
      .addTo(m);
  }, [shownId, rows]);

  // Bring a newly selected flat into view if it is off screen.
  useEffect(() => {
    const m = map.current;
    const row = rowsRef.current.find((x) => x.item.flat.id === selectedId);
    if (!m || !row) return;
    const ll: [number, number] = [row.item.flat.longitude, row.item.flat.latitude];
    const p = m.project(ll);
    const c = m.getContainer();
    if (p.x < 40 || p.y < 40 || p.x > c.clientWidth - 40 || p.y > c.clientHeight - 40 - padBottom) {
      m.easeTo({ center: ll, duration: 450, offset: [0, -padBottom / 2] });
    }
  }, [selectedId, padBottom]);

  return (
    <div className="relative h-full w-full">
      <div ref={container} className="h-full w-full" />
      <div className="pointer-events-none absolute left-3 top-3 max-w-[calc(100%-130px)] rounded-xl md:max-w-none bg-white/95 px-3 py-2 text-[11px] text-ink-600 shadow-card ring-1 ring-ink-900/5" aria-hidden>
        <div className="flex items-center gap-2">
          <span className="rank-marker-legend">1</span>
          <span>Top {TOP_N} by match, numbered</span>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1">
          <span className="font-medium text-ink-700">Colour = Livability</span>
          <span>Low</span>
          <span className="h-2 w-10 rounded-full md:w-14" style={{ background: `linear-gradient(90deg, ${[20, 45, 65, 80, 95].map(scoreColor).join(',')})` }} />
          <span>High</span>
        </div>
      </div>
    </div>
  );
}
