import { useAuth } from '../state/authStore';
import { useBookmarks } from '../state/bookmarkStore';
import { toast } from '../state/toastStore';

/** After sign-in, load bookmarks and complete whatever the user was doing (UC-11 step 6). */
export async function finishPendingAction() {
  const { pending, setPending } = useAuth.getState();
  const bookmarks = useBookmarks.getState();
  await bookmarks.load().catch(() => undefined);
  if (!pending) return;
  setPending(null);
  if (!useBookmarks.getState().ids.has(pending.flatId)) {
    try {
      await useBookmarks.getState().toggle(pending.flatId);
      toast('Saved to bookmarks', 'success');
    } catch {
      toast('Could not save the bookmark', 'error');
    }
  }
}
