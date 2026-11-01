import { create } from 'zustand';
import { api } from '../api/apiClient';

/** Bookmarked flat ids for the star icons, plus the flats picked for comparison (UC-08, UC-09). */
interface BookmarkState {
  ids: Set<number>;
  compare: number[];
  load: () => Promise<void>;
  toggle: (flatId: number) => Promise<'added' | 'removed'>;
  setCompare: (ids: number[]) => void;
  toggleCompare: (id: number) => void;
  reset: () => void;
}

// Bumped by every add/remove, so a list fetched before a change cannot overwrite it when it arrives late
// (e.g. the load on sign-in racing the pending bookmark completed after sign-up).
let changes = 0;

// The flats ticked for Compare survive a reload of /compare or /bookmarks (same tab only, like the search form).
const COMPARE_KEY = 'hm.compare';
function readCompare(): number[] {
  try {
    const v = JSON.parse(sessionStorage.getItem(COMPARE_KEY) ?? '[]');
    return Array.isArray(v) ? v.filter((x): x is number => typeof x === 'number') : [];
  } catch {
    return [];
  }
}
function saveCompare(ids: number[]) {
  try {
    if (ids.length) sessionStorage.setItem(COMPARE_KEY, JSON.stringify(ids));
    else sessionStorage.removeItem(COMPARE_KEY);
  } catch {
    /* private mode */
  }
}

export const useBookmarks = create<BookmarkState>((set, get) => ({
  ids: new Set(),
  compare: readCompare(),
  load: async () => {
    const at = changes;
    const ids = await api.bookmarkIds();
    if (at !== changes) return;
    set({ ids: new Set(ids), compare: get().compare.filter((c) => ids.includes(c)) });
  },
  toggle: async (flatId) => {
    if (get().ids.has(flatId)) {
      await api.removeBookmark(flatId);
      changes++;
      const ids = new Set(get().ids);
      ids.delete(flatId);
      set({ ids, compare: get().compare.filter((c) => c !== flatId) });
      return 'removed';
    }
    try {
      await api.addBookmark(flatId);
    } catch (e) {
      // UC-08.EX.1: already saved elsewhere (another tab); just reflect it.
      if (!(e instanceof Error && 'code' in e && (e as { code: string }).code === 'ALREADY_BOOKMARKED')) throw e;
    }
    changes++;
    set({ ids: new Set(get().ids).add(flatId) });
    return 'added';
  },
  setCompare: (compare) => set({ compare }),
  toggleCompare: (id) => {
    const c = get().compare;
    set({ compare: c.includes(id) ? c.filter((x) => x !== id) : [...c, id] });
  },
  reset: () => set({ ids: new Set(), compare: [] }),
}));

useBookmarks.subscribe((s, prev) => {
  if (s.compare !== prev.compare) saveCompare(s.compare);
});
