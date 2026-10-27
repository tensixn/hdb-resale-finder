import type {
  ApiErrorBody, AuthResponse, Bookmark, CompareResult, CpfResult, FinancialProfile, Filters, FlatDetail,
  LivabilityFactor, Profile, SearchRequest, SearchResult, SystemStatus, Valuation, Weights,
} from '../types/models';

const TOKEN_KEY = 'hm.token';

/** An error returned by the backend, with optional per-field messages for forms. */
export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string, public fieldErrors: Record<string, string> = {}) {
    super(message);
  }
}

let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

export function getToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage unavailable: the session simply will not survive a reload */
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Check that the backend is running.');
  }
  // The backend slides the session while the user is active and hands back a fresh token.
  const renewed = res.headers.get('X-Access-Token');
  if (renewed) setToken(renewed);

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    const err = (data ?? {}) as Partial<ApiErrorBody>;
    if (res.status === 401 && token && err.code === 'AUTH_REQUIRED') onUnauthorized?.();
    throw new ApiError(res.status, err.code ?? 'ERROR', err.message ?? `Request failed (${res.status})`, err.fieldErrors ?? {});
  }
  return data as T;
}

export const api = {
  search: (req: SearchRequest) => request<SearchResult>('POST', '/search', req),
  view: (searchId: string, filters: Filters & { weights: Weights }) =>
    request<SearchResult>('POST', `/search/${searchId}/view`, filters),

  currentWeights: () => request<Weights>('GET', '/livability/weights'),
  adjustWeights: (factor: LivabilityFactor, value: number, current: Weights) =>
    request<Weights>('POST', '/livability/weights/adjust', { factor, value, current }),
  resetWeights: () => request<Weights>('POST', '/livability/weights/reset'),
  replaceWeights: (weights: Weights) => request<Weights>('PUT', '/livability/weights', weights),

  flat: (id: number, searchId?: string | null, w?: Weights) => {
    const q = new URLSearchParams();
    if (searchId) q.set('searchId', searchId);
    if (w) {
      q.set('lease', String(w.lease));
      q.set('safety', String(w.safety));
      q.set('density', String(w.density));
      q.set('airQuality', String(w.airQuality));
    }
    return request<FlatDetail>('GET', `/flats/${id}?${q}`);
  },
  valuation: (id: number) => request<Valuation>('GET', `/flats/${id}/valuation`),

  savedFinancialProfile: () => request<FinancialProfile | undefined>('GET', '/cpf/profile'),
  estimateCpf: (body: {
    flatId: number;
    profile?: FinancialProfile;
    saveProfile: boolean;
    loanTenureYears?: number;
    downPaymentPercent?: number;
    interestRatePercent?: number;
  }) => request<CpfResult>('POST', '/cpf/estimate', body),

  bookmarks: () => request<Bookmark[]>('GET', '/bookmarks'),
  bookmarkIds: () => request<number[]>('GET', '/bookmarks/ids'),
  addBookmark: (flatId: number) => request<void>('POST', '/bookmarks', { flatId }),
  removeBookmark: (flatId: number) => request<void>('DELETE', `/bookmarks/${flatId}`),
  compare: (flatIds: number[], searchId?: string | null, weights?: Weights) =>
    request<CompareResult>('POST', '/compare', { flatIds, searchId, weights }),

  login: (email: string, password: string) => request<AuthResponse>('POST', '/auth/login', { email, password }),
  register: (email: string, password: string, confirmPassword: string, consent: boolean) =>
    request<AuthResponse>('POST', '/auth/register', { email, password, confirmPassword, consent }),
  logout: () => request<void>('POST', '/auth/logout'),

  profile: () => request<Profile>('GET', '/profile'),
  saveFinancial: (p: FinancialProfile) => request<Profile>('PUT', '/profile/financial', p),
  deleteFinancial: () => request<void>('DELETE', '/profile/financial'),

  status: () => request<SystemStatus>('GET', '/system/status'),
  options: () => request<{ towns: string[]; flatTypes: string[] }>('GET', '/system/options'),
  places: (q: string) => request<string[]>('GET', `/system/places?q=${encodeURIComponent(q)}`),
  refreshValuation: () => request<{ status: string; recordsIngested: number; message: string }>('POST', '/system/valuation-refresh'),
};
