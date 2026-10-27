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

export const useBookmarks = create<BookmarkState>((set, get) => ({
  ids: new Set(),
  compare: [],
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
