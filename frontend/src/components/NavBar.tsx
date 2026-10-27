import { Bookmark, CircleHelp, Moon, Search, Sun, UserRound } from './icons';
import { useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../state/authStore';
import { useSearch } from '../state/searchStore';

const tabs = [
  { to: '/', label: 'Search', icon: Search, match: (p: string) => p === '/' || p.startsWith('/results') || p.startsWith('/flats') },
  { to: '/bookmarks', label: 'Bookmarks', icon: Bookmark, match: (p: string) => p.startsWith('/bookmarks') || p.startsWith('/compare') },
  { to: '/profile', label: 'Profile', icon: UserRound, match: (p: string) => p.startsWith('/profile') || p.startsWith('/login') || p.startsWith('/signup') },
];

export function Logo() {
  return (
    <Link to="/" className="flex min-h-[44px] items-center gap-2.5 rounded-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600">
      <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden>
        <defs>
          <mask id="logo-windows">
            <rect width="32" height="32" fill="#fff" />
            <path d="M9 9h2.5v2.5H9zM13.5 9H16v2.5h-2.5zM9 14h2.5v2.5H9zM13.5 14H16v2.5h-2.5zM9 19h2.5v2.5H9zM13.5 19H16v2.5h-2.5z" fill="#000" />
          </mask>
        </defs>
        <rect x="6" y="3" width="12" height="26" rx="1.5" fill="#1d7a6b" mask="url(#logo-windows)" />
        <rect x="20" y="13" width="7" height="16" rx="1.5" fill="#7fcfbd" />
        <rect x="3" y="28" width="26" height="1.5" rx=".75" fill="#1d7a6b" />
      </svg>
      <div className="leading-tight">
        <div className="font-display text-[17px] font-semibold tracking-tight text-ink-900">HomeMatch SG</div>
        <div className="hidden text-[11px] text-ink-500 sm:block">HDB resale flat search &amp; analysis</div>
      </div>
    </Link>
  );
}

function ThemeToggle() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'));
  const toggle = () => {
    const next = !dark;
    document.documentElement.classList.toggle('dark', next);
    try { localStorage.theme = next ? 'dark' : 'light'; } catch { /* private mode */ }
    setDark(next);
  };
  return (
    <button type="button" onClick={toggle} aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'} className="flex h-11 w-11 items-center justify-center rounded-lg text-ink-500 hover:bg-ink-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 md:h-9 md:w-9">
      {dark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
    </button>
  );
}

/** Top bar on wider screens; on phones the same three tabs move to a bottom bar (SRS 3.1). */
export function NavBar() {
  const { pathname } = useLocation();
  const email = useAuth((s) => s.email);
  const hasResults = useSearch((s) => s.searchId !== null);
  return (
    <>
      <header className="sticky top-0 z-40 border-b border-ink-900/5 bg-white/90 backdrop-blur dark:bg-ink-100/90">
        <div className="mx-auto flex h-16 max-w-[1600px] items-center justify-between px-4 lg:px-6">
          <Logo />
          <nav className="hidden items-center gap-1 md:flex">
            {tabs.map((t) => {
              const to = t.to === '/' && hasResults ? '/results' : t.to;
              const active = t.match(pathname);
              return (
                <NavLink
                  key={t.to}
                  to={to}
                  className={`flex items-center gap-2 rounded-xl px-3.5 py-2 text-sm font-medium transition ${
                    active ? 'bg-brand-50 text-brand-700' : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900'
                  }`}
                >
                  <t.icon className="h-4 w-4" />
                  {t.label}
                </NavLink>
              );
            })}
            <NavLink
              to="/help"
              className={`ml-1 flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium ${
                pathname === '/help' ? 'bg-brand-50 text-brand-700' : 'text-ink-500 hover:bg-ink-100'
              }`}
            >
              <CircleHelp className="h-4 w-4" /> Help
            </NavLink>
          </nav>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            {email ? (
              <Link to="/profile" className="flex items-center gap-2 rounded-full bg-ink-100 py-1 pl-1 pr-3 text-sm text-ink-700 hover:bg-ink-200">
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-600 text-xs font-bold uppercase text-white">
                  {email[0]}
                </span>
                <span className="hidden max-w-[160px] truncate lg:inline">{email}</span>
              </Link>
            ) : (
              <Link to="/login" className="flex min-h-[44px] items-center rounded-xl px-3.5 text-sm font-semibold text-brand-700 hover:bg-brand-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 md:min-h-[36px]">
                Sign in
              </Link>
            )}
            <Link to="/help" className="flex h-11 w-11 items-center justify-center rounded-lg text-ink-500 hover:bg-ink-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 md:hidden" aria-label="Help">
              <CircleHelp className="h-5 w-5" />
            </Link>
          </div>
        </div>
      </header>
      <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-ink-900/10 bg-white dark:bg-ink-100 pb-[env(safe-area-inset-bottom)] md:hidden">
        <div className="grid grid-cols-3">
          {tabs.map((t) => {
            const to = t.to === '/' && hasResults ? '/results' : t.to;
            const active = t.match(pathname);
            return (
              <NavLink key={t.to} to={to} className={`flex flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium ${active ? 'text-brand-700' : 'text-ink-500'}`}>
                <t.icon className="h-5 w-5" />
                {t.label}
              </NavLink>
            );
          })}
        </div>
      </nav>
    </>
  );
}
