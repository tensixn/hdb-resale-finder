/** Types mirroring the backend REST DTOs. */

export type TransportMode = 'PUBLIC_TRANSPORT' | 'DRIVE' | 'CYCLE' | 'WALK';
export type ValuationLabel = 'UNDER_VALUED' | 'FAIR_VALUED' | 'OVER_VALUED' | 'INSUFFICIENT_DATA';
export type MaritalStatus = 'SINGLE' | 'MARRIED' | 'ENGAGED';
export type LivabilityFactor = 'LEASE' | 'SAFETY' | 'DENSITY' | 'AIR_QUALITY';

export interface Weights {
  lease: number;
  safety: number;
  density: number;
  airQuality: number;
}

export interface Anchor {
  index: number;
  query: string;
  label: string;
  latitude: number;
  longitude: number;
  approximate: boolean;
}

export interface FlatSummary {
  id: number;
  block: string;
  street: string;
  town: string;
  flatType: string;
  floorAreaSqm: number;
  storeyRange: string;
  leaseCommenceYear: number;
  remainingLeaseYears: number;
  price: number;
  priceMonth: string;
  latitude: number;
  longitude: number;
}

export interface Livability {
  index: number;
  lease: number;
  safety: number;
  density: number;
  airQuality: number;
}

export interface CommuteLeg {
  anchorIndex: number;
  mode: TransportMode;
  minutes: number;
  score: number;
}

export interface RankedFlat {
  rank: number;
  flat: FlatSummary;
  livability: Livability;
  commute: CommuteLeg[];
  commuteScore: number;
  matchScore: number;
  valuation: ValuationLabel;
  commuteOutdated: boolean;
}

export interface SearchRequest {
  workplace1: string;
  workplace2?: string;
  transportModes: TransportMode[];
  minPrice?: number | null;
  maxPrice?: number | null;
  flatTypes: string[];
  towns: string[];
  weights?: Weights;
}

export interface Filters {
  minPrice?: number | null;
  maxPrice?: number | null;
  flatTypes: string[];
  towns: string[];
}

export interface SearchResult {
  searchId: string;
  anchors: Anchor[];
  weights: Weights;
  results: RankedFlat[];
  totalMatched: number;
  totalShown: number;
  excluded: number;
  warnings: string[];
  suggestions: string[];
}

export interface Valuation {
  label: ValuationLabel;
  price: number | null;
  fairValue: number | null;
  rangeLow: number | null;
  rangeHigh: number | null;
  estateAveragePrice: number | null;
  estateAveragePsm: number | null;
  sampleSize: number | null;
  windowStart: string | null;
  windowEnd: string | null;
  baselineDate: string | null;
  fairBandPercent: number;
  stale: boolean;
  message: string | null;
}

export interface FlatDetail {
  flat: FlatSummary;
  region: string;
  livability: Livability;
  context: { crimeRatePerThousand: number; residentsPerHa: number; psi: number | null };
  commute: CommuteLeg[] | null;
  anchors: Anchor[];
  valuation: Valuation;
  nearbySchools: { name: string; distanceKm: number }[];
  transactions: { month: string; price: number }[];
  bookmarked: boolean;
  warnings: string[];
}

export interface FinancialProfile {
  householdIncome: number | null;
  maritalStatus: MaritalStatus | null;
  parentPostalCode: string;
  livingWithParents: boolean;
}

export interface CpfGrant {
  scheme: 'CHG' | 'EHG' | 'PHG';
  amount: number;
  reason: string;
}

export interface CpfResult {
  price: number;
  grants: CpfGrant[];
  totalGrants: number;
  loan: {
    lender: string;
    interestRatePercent: number;
    tenureYears: number;
    downPaymentPercent: number;
    maxLoanToValuePercent: number;
  };
  downPayment: number;
  loanAmount: number;
  monthlyInstalment: number;
  affordability: { rating: 'COMFORTABLE' | 'STRETCHED' | 'ABOVE_LIMIT' | 'UNKNOWN'; instalmentToIncomePercent: number | null; message: string };
  parentDistanceKm: number | null;
  profileSaved: boolean;
  disclaimers: string[];
}

export interface Bookmark {
  flat: FlatSummary;
  livability: Livability;
  valuation: ValuationLabel;
  createdAt: string;
}

export interface CompareRow {
  flat: FlatSummary;
  pricePerSqm: number;
  livability: Livability;
  valuation: ValuationLabel;
  fairValue: number | null;
  commute: CommuteLeg[] | null;
  commuteScore: number | null;
}

export interface CompareResult {
  anchors: Anchor[];
  flats: CompareRow[];
  best: Record<string, number>;
}

export interface Profile {
  email: string;
  memberSince: string;
  weights: Weights;
  financial: FinancialProfile | null;
  financialUpdatedAt: string | null;
  lastSearch: {
    workplace1: string;
    workplace2: string | null;
    transportModes: string;
    minPrice: number | null;
    maxPrice: number | null;
    flatTypes: string | null;
    towns: string | null;
    at: string;
  } | null;
}

export interface AuthResponse {
  token: string;
  email: string;
  expiresAt: string;
}

export interface SystemStatus {
  dataMode: 'offline' | 'live';
  flats: number;
  transactions: number;
  latestTransactionMonth: string | null;
  lastRefreshAt: string | null;
  lastRefreshStatus: string | null;
  lastRefreshRecords: number | null;
  baselineComputedAt: string | null;
  manualRefreshAllowed: boolean;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}
