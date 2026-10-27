import { create } from 'zustand';
import { api, ApiError } from '../api/apiClient';
import type { Filters, LivabilityFactor, SearchRequest, SearchResult, TransportMode, Weights } from '../types/models';

const FORM_KEY = 'hm.search';
const DEFAULT_WEIGHTS: Weights = { lease: 25, safety: 25, density: 25, airQuality: 25 };

export interface SearchForm {
  workplace1: string;
  workplace2: string;
  transportModes: TransportMode[];
  minPrice: string;
  maxPrice: string;
  flatTypes: string[];
  towns: string[];
}

const emptyForm: SearchForm = {
  workplace1: '',
  workplace2: '',
  transportModes: ['PUBLIC_TRANSPORT'],
  minPrice: '',
  maxPrice: '',
  flatTypes: [],
  towns: [],
};

interface Persisted {
  form: SearchForm;
  weights: Weights;
  searchId: string | null;
  filters: Filters;
}

function load(): Persisted {
  try {
    const raw = sessionStorage.getItem(FORM_KEY);
    if (raw) return JSON.parse(raw) as Persisted;
  } catch {
    /* ignore */
  }
  return { form: emptyForm, weights: DEFAULT_WEIGHTS, searchId: null, filters: { flatTypes: [], towns: [] } };
}

function save(s: Persisted) {
  try {
    sessionStorage.setItem(FORM_KEY, JSON.stringify(s));
  } catch {
    /* ignore */
  }
}

/** Client-side filters the backend does not offer (minimum lease left, minimum floor area). */
export interface ExtraFilters {
  minLease: number | null;
  minArea: number | null;
}
const EXTRA_KEY = 'hm.search.extra';
const emptyExtra: ExtraFilters = { minLease: null, minArea: null };
function loadExtra(): ExtraFilters {
  try {
    const raw = sessionStorage.getItem(EXTRA_KEY);
    if (raw) return { ...emptyExtra, ...(JSON.parse(raw) as ExtraFilters) };
  } catch {
    /* ignore */
  }
  return emptyExtra;
}

/** Ranks before the last change of priorities, so the results can show what moved and why. */
export interface RankBaseline {
  weights: Weights;
  ranks: Record<number, number>;
  topId: number | null;
}

const toNumber = (s: string) => {
  const n = Number(s.replace(/[^\d]/g, ''));
  return s.trim() === '' || Number.isNaN(n) ? null : n;
};

interface SearchState extends Persisted {
  result: SearchResult | null;
  loading: boolean;
  refreshing: boolean;
  error: string | null;
  fieldErrors: Record<string, string>;
  hoveredFlatId: number | null;
  selectedFlatId: number | null;
  setForm: (patch: Partial<SearchForm>) => void;
  runSearch: () => Promise<boolean>;
  restore: () => Promise<void>;
  setFilters: (f: Filters) => Promise<void>;
  adjustWeight: (factor: LivabilityFactor, value: number) => Promise<void>;
  setWeights: (w: Weights) => Promise<void>;
  resetWeights: () => Promise<void>;
  /** Applies a whole preset at once; normalised and saved for signed-in users by the server. */
  applyWeights: (w: Weights) => Promise<void>;
  setHovered: (id: number | null) => void;
  setSelected: (id: number | null) => void;
  clear: () => void;
  extraFilters: ExtraFilters;
  setExtraFilters: (f: ExtraFilters) => void;
  rankBaseline: RankBaseline | null;
  setRankBaseline: (b: RankBaseline | null) => void;
}

let weightSeq = 0;

export const useSearch = create<SearchState>((set, get) => {
  const persist = () => {
    const { form, weights, searchId, filters } = get();
    save({ form, weights, searchId, filters });
  };

  const refreshView = async () => {
    const { searchId, filters, weights } = get();
    if (!searchId) return;
    set({ refreshing: true });
    try {
      const result = await api.view(searchId, { ...filters, weights });
      set({ result, refreshing: false });
    } catch (e) {
      set({ refreshing: false });
      if (e instanceof ApiError && e.code === 'SEARCH_EXPIRED') set({ searchId: null, result: null });
      else throw e;
    }
  };

  return {
    ...load(),
    result: null,
    loading: false,
    refreshing: false,
    error: null,
    fieldErrors: {},
    hoveredFlatId: null,
    selectedFlatId: null,

    setForm: (patch) => {
      set((s) => ({ form: { ...s.form, ...patch }, fieldErrors: {} }));
      persist();
    },

    runSearch: async () => {
      const { form, weights } = get();
      if (!form.workplace1.trim()) {
        set({ fieldErrors: { workplace1: 'Enter Workplace 1 to search.' } });
        return false;
      }
      const req: SearchRequest = {
        workplace1: form.workplace1.trim(),
        workplace2: form.workplace2.trim() || undefined,
        transportModes: form.transportModes,
        minPrice: toNumber(form.minPrice),
        maxPrice: toNumber(form.maxPrice),
        flatTypes: form.flatTypes,
        towns: form.towns,
        weights,
      };
      set({ loading: true, error: null, fieldErrors: {} });
      try {
        const result = await api.search(req);
        set({ result, searchId: result.searchId, weights: result.weights, filters: { flatTypes: [], towns: [] }, loading: false });
        persist();
        return true;
      } catch (e) {
        const err = e as ApiError;
        set({ loading: false, error: err.message, fieldErrors: err.fieldErrors ?? {} });
        return false;
      }
    },

    restore: async () => {
      if (get().result || !get().searchId) return;
      try {
        await refreshView();
      } catch {
        set({ searchId: null });
      }
      persist();
    },

    setFilters: async (filters) => {
      set({ filters });
      persist();
      await refreshView();
    },

    // UC-03: the server rebalances to 100% (FR-6), saves for signed-in users (FR-21), then we re-rank.
    adjustWeight: async (factor, value) => {
      const seq = ++weightSeq;
      const weights = await api.adjustWeights(factor, value, get().weights);
      if (seq !== weightSeq) return;
      set({ weights });
      persist();
      await refreshView();
    },

    setWeights: async (weights) => {
      set({ weights });
      persist();
      await refreshView();
    },

    applyWeights: async (w) => {
      const seq = ++weightSeq;
      const weights = await api.replaceWeights(w);
      if (seq !== weightSeq) return;
      set({ weights });
      persist();
      await refreshView();
    },

    resetWeights: async () => {
      const weights = await api.resetWeights();
      set({ weights });
      persist();
      await refreshView();
    },

    setHovered: (hoveredFlatId) => set({ hoveredFlatId }),
    setSelected: (selectedFlatId) => set({ selectedFlatId }),
    extraFilters: loadExtra(),
    setExtraFilters: (extraFilters) => {
      set({ extraFilters });
      try {
        sessionStorage.setItem(EXTRA_KEY, JSON.stringify(extraFilters));
      } catch {
        /* ignore */
      }
    },
    rankBaseline: null,
    setRankBaseline: (rankBaseline) => set({ rankBaseline }),
    clear: () => {
      set({ result: null, searchId: null, filters: { flatTypes: [], towns: [] } });
      persist();
    },
  };
});

export { DEFAULT_WEIGHTS };
