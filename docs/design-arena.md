# Design arena: how the UI was chosen

On 2026-10-23 the core screens (search, results, flat detail) were built three times in parallel against the same
backend, each with a different design direction, then scored against one rubric by an independent reviewer and by
the integrator.

| Candidate | Direction | Reviewer score (of 30) |
|---|---|---|
| 1 | Explain the ranking: map-first, every rank decomposed into points | **27** |
| 2 | Shortlist ledger: list-first table, map secondary, inline pinning | 24 |
| 3 | Guided by priorities: preset-driven search, phone-first | 18 |

Rubric: fast path to results; explains the ranking; re-weighting and filter feedback; decision-oriented detail;
phone quality at 390px; code health and constraints.

## Base: candidate 1
Both judges picked it. The ranking and its explanations come from one pure function (`lib/explain.ts`,
`lib/useRanking.ts`) shared by the results and detail screens, so new explanations are added in one place. It had
no hidden side effects and only extended the stores.

## Grafted
- **Priority presets** (from 3): Balanced, Long lease, Quiet & green, Safe & sound, on the search screen and in the
  Priorities panel; the toolbar names the active mix. Reworked so presets are saved for signed-in users through a
  new `PUT /api/livability/weights` endpoint (FR-21), which candidate 3 skipped.
- **Bookmark race fix** (from 3): a bookmark list that arrives late after sign-in can no longer overwrite the
  bookmark just completed.
- **"The short version"** (from 2): best overall, shortest commute, lowest price and most livable, each one tap from
  selecting that flat. On phones it sits below the top three cards so the #1 match stays above the fold.
- **Compact phone header** (idea from 3): "How the order is decided" is collapsed by default on phones.

## Rejected
- Candidate 3's live preview: it ran a real search on every keystroke, which overwrote a signed-in user's saved
  search (sometimes with a placeholder workplace).
- Candidate 3's "Short commute" preset: commute is fixed at 50% of the score, so it worked by silently hiding flats
  over 45 minutes. A preset should not remove results the user did not filter out.
- Candidate 3's map: a MapLibre expression error meant no flat dots were drawn.
- Candidate 2's compare-by-bookmarking (a hidden side effect), double-rendered rows, and serif ledger styling,
  which would have meant restyling every other page to match.
- Candidate 2's "Your brief" sentence and "vs #1" line: candidate 1's live formula bar and neighbour comparison
  already carry that information.

## Verification
Typecheck, build, 26 backend tests and 3 frontend tests pass. `qa/e2e.mjs` passes all 20 steps at 1440x900 and
390x844 against the bundled jar with no console errors and no server errors.
