import { create } from 'zustand';
import { api, getToken, setToken } from '../api/apiClient';

const EMAIL_KEY = 'hm.email';

/** An action the user tried before signing in, completed after UC-11 returns (step 6). */
export type PendingAction = { type: 'bookmark'; flatId: number } | { type: 'compare'; flatId: number };

interface AuthState {
  email: string | null;
  name: string | null;
  setName: (name: string | null) => void;
  pending: PendingAction | null;
  signedIn: () => boolean;
  signIn: (token: string, email: string) => void;
  signOut: () => Promise<void>;
  clearLocal: () => void;
  setPending: (p: PendingAction | null) => void;
}

function readEmail(): string | null {
  try {
    return getToken() ? sessionStorage.getItem(EMAIL_KEY) : null;
  } catch {
    return null;
  }
}

export const useAuth = create<AuthState>((set, get) => ({
  email: readEmail(),
  name: null,
  setName: (name) => set({ name }),
  pending: null,
  signedIn: () => get().email !== null,
  signIn: (token, email) => {
    setToken(token);
    try {
      sessionStorage.setItem(EMAIL_KEY, email);
    } catch {
      /* ignore */
    }
    set({ email });
  },
  // UC-15: end the session on the server, then clear everything personal held in the browser.
  signOut: async () => {
    try {
      await api.logout();
    } catch {
      /* UC-15.EX.1: if the server is unreachable we still clear locally; the token expires on its own */
    }
    get().clearLocal();
  },
  clearLocal: () => {
    setToken(null);
    try {
      sessionStorage.removeItem(EMAIL_KEY);
      sessionStorage.removeItem('hm.cpf');
    } catch {
      /* ignore */
    }
    set({ email: null, name: null, pending: null });
  },
  setPending: (pending) => set({ pending }),
}));
