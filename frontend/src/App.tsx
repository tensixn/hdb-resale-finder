import { useEffect } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { api, setUnauthorizedHandler } from './api/apiClient';
import { NavBar } from './components/NavBar';
import { Toasts } from './components/Toasts';
import BookmarksScreen from './pages/BookmarksScreen';
import CompareScreen from './pages/CompareScreen';
import FlatDetailScreen from './pages/FlatDetailScreen';
import HelpScreen from './pages/HelpScreen';
import LoginScreen from './pages/LoginScreen';
import ProfileScreen from './pages/ProfileScreen';
import ResultsMapListScreen from './pages/ResultsMapListScreen';
import SearchPrioritiesScreen from './pages/SearchPrioritiesScreen';
import SignupScreen from './pages/SignupScreen';
import { useAuth } from './state/authStore';
import { useBookmarks } from './state/bookmarkStore';
import { toast } from './state/toastStore';

/** Screens that need a signed-in session (FR-24): guests are sent to Login and brought back afterwards. */
function RequireAuth({ children }: { children: JSX.Element }) {
  const email = useAuth((s) => s.email);
  const location = useLocation();
  if (!email) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  return children;
}

export default function App() {
  const email = useAuth((s) => s.email);
  const clearLocal = useAuth((s) => s.clearLocal);
  const setName = useAuth((s) => s.setName);
  const loadBookmarks = useBookmarks((s) => s.load);
  const resetBookmarks = useBookmarks((s) => s.reset);
  const navigate = useNavigate();

  useEffect(() => {
    if (!email) return;
    loadBookmarks().catch(() => undefined);
    api.profile().then((p) => setName(p.displayName)).catch(() => undefined);
  }, [email, loadBookmarks, setName]);

  // UC-15.AC.1: an expired session is treated as signed out.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearLocal();
      resetBookmarks();
      toast('Your session has ended. Please sign in again.', 'info');
      navigate(`/login?next=${encodeURIComponent(window.location.pathname)}`);
    });
  }, [clearLocal, resetBookmarks, navigate]);

  return (
    <div className="min-h-dvh">
      <NavBar />
      <main>
        <Routes>
          <Route path="/" element={<SearchPrioritiesScreen />} />
          <Route path="/results" element={<ResultsMapListScreen />} />
          <Route path="/flats/:id" element={<FlatDetailScreen />} />
          <Route path="/bookmarks" element={<RequireAuth><BookmarksScreen /></RequireAuth>} />
          <Route path="/compare" element={<RequireAuth><CompareScreen /></RequireAuth>} />
          <Route path="/profile" element={<RequireAuth><ProfileScreen /></RequireAuth>} />
          <Route path="/login" element={<LoginScreen />} />
          <Route path="/signup" element={<SignupScreen />} />
          <Route path="/help" element={<HelpScreen />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Toasts />
    </div>
  );
}
