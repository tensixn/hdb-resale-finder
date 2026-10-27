import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/apiClient';
import { useAuth } from '../state/authStore';
import { useBookmarks } from '../state/bookmarkStore';
import { toast } from '../state/toastStore';

/**
 * Bookmark toggle that includes Authenticate User (UC-08 step 2): guests are sent to sign in, and the
 * bookmark is completed when they come back (UC-11 step 6).
 */
export function useBookmarkAction() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = useAuth((s) => s.email);
  const setPending = useAuth((s) => s.setPending);
  const toggle = useBookmarks((s) => s.toggle);

  return useCallback(
    async (flatId: number) => {
      if (!email) {
        setPending({ type: 'bookmark', flatId });
        navigate(`/login?next=${encodeURIComponent(location.pathname + location.search)}`);
        return;
      }
      try {
        const r = await toggle(flatId);
        toast(r === 'added' ? 'Saved to bookmarks' : 'Removed from bookmarks', 'success');
      } catch (e) {
        toast(e instanceof ApiError ? e.message : 'Could not update bookmarks', 'error');
      }
    },
    [email, setPending, navigate, location, toggle],
  );
}
