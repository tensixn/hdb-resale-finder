## Full UI audit — HomeMatch SG frontend (all 10 screens)

Input: live page (local backend in offline mode + Vite dev server) and source, repo HEAD `75f1fa5` ·
Stages run: accessibility-review, better-interface, find-animation-opportunities · Not run: none

Every screen (search, search with errors, results, flat detail, login, signup, help, bookmarks, compare, profile)
was checked in light and dark mode at 1440px and 390px wide, which makes 40 page states. Open states were
checked too: the towns menu, place suggestions, the Priorities and Filters panels, an expanded score breakdown,
the CPF panel, a populated compare table and the phone map. Tools: axe-core 4.10.2 for a first sweep, the
WCAG luminance formula for contrast, measured bounding boxes for target size, and a scripted Tab walk through
search and results.

### Verdict

**Before fixes: Block.** 307 failing text elements across the 40 page states, plus a slider focus ring that
could barely be seen, and toasts that screen readers never announced. The fix with the most leverage was
retuning the `--ink-400/500` tokens and adding dark-mode patches for the status colours, which cleared most of
the 307.

**After the fixes in this branch: Needs changes.** axe now reports 0 violations on all 40 states and on every
open state. The remaining items are Warnings or Advisories (listed below), plus motion suggestions, which never
block.

### 1. Accessibility (accessibility-review)

| Check | WCAG | Before | After | Detail |
|---|---|---|---|---|
| Colour contrast | 1.4.3 | ❌ FAIL | ✅ PASS | Worst before: 1.71:1 (`text-brand-800` on dark `bg-brand-50`). Light `ink-400` #8791a7 on white measured 3.16:1. Dark `ink-500` #68738c on cards measured 3.22:1 |
| Non-text contrast | 1.4.11 | ❌ FAIL | ✅ PASS | Slider focus was a 25% tint (<3:1). Dark-mode `ring-brand-600` focus rings measured 2.96:1 |
| Target size | 2.5.8 | ❌ FAIL | ✅ PASS | (i) buttons were 14×14 on desktop and are now 24×24. Login "Cancel" was 45×20 and is now 44px tall |
| Reflow | 1.4.10 | ✅ PASS | ✅ PASS | No horizontal scroll at 320px on search, detail or profile |
| Use of colour | 1.4.1 | ✅ PASS | ✅ PASS | Valuation badges carry text, and best cells in compare show a crown |
| Names and labels | 1.1.1 / 4.1.2 / 3.3.2 | ❌ FAIL | ✅ PASS | Towns menu had no programmatic label and no selected state. The place field was not a combobox. Rank cards used `aria-expanded` on `<article>`, which that role does not support |
| Keyboard and focus | 2.1.1 / 2.4.3 / 2.4.7 | ❌ FAIL | ✅ PASS | Escape did not close the towns menu or the (i) tips. On phones, step 4 came after the Search button |
| Status messages | 4.1.3 | ❌ FAIL | ✅ PASS | Toasts had no live region |
| Page language and title | 3.1.1 / 2.4.2 | ⚠️ | ✅ PASS | `lang="en"` was already set. Every route shared one `<title>`; now each route has its own |

**Blockers (all fixed in this branch)**

| # | Element | Location | Issue | Fix applied |
|---|---|---|---|---|
| 1 | Secondary and hint text, every screen | `src/index.css` `:root`/`.dark` ink tokens | Light `ink-400` 3.16:1 and `ink-500` 4.43:1 on `ink-50`. Dark `ink-400` 2.29:1, `ink-500` 3.22:1 and `ink-600` 4.41:1 | Light 400 → #677189 (4.56:1 on ink-50), 500 → #5b6680 (4.94:1 on ink-100). Dark 400 → #8f98ad (4.7:1), 500 → #a3abbf, 600/700 shifted up one step |
| 2 | Status colours in dark mode (valuation badges, score deltas, amber callout, toolbars, compare tray) | `src/index.css` dark patch layer | Colours with no dark override, e.g. `text-rose-800` on `#3b1219` at 2.03:1, `text-sky-800` at 2.02:1, `text-emerald-700` at 2.79:1, and the compare-tray text at 1.8:1 on an unpatched `bg-white/95` | Added dark overrides for amber, emerald, sky, rose, the deeper brand text shades, `bg-white/95` and the slider track |
| 3 | 10px labels on the 100-point bar | `components/RankingFormula.tsx` | White on orange 2.75:1, on blue 3.04:1, on olive 4.44:1 | Label colour now follows the segment's luminance (near-black on light segments). Safety olive changed #6b7f3a → #637634 (5.0:1) |
| 4 | Livability sliders | `src/index.css` range focus | A 25% alpha halo on the thumb as the only focus cue | 2px solid `#1d7a6b` outline with a 2px offset (`#4bb39f` in dark) |
| 5 | Dark-mode focus rings | `src/index.css` | `ring-brand-600`/`500` measured 2.96:1 on dark cards | Swapped to `#4bb39f` in dark (6.0:1) |
| 6 | Toasts | `components/Toasts.tsx` | Session-expired, re-ranked and bookmark messages were never announced | Always-mounted `role="status" aria-live="polite"` container |
| 7 | (i) info tips | `components/ui.tsx` InfoTip | 14×14 target on desktop. Escape did not dismiss. The tip closed when the pointer moved onto it | 24×24 target, Escape listener, hover handlers moved to the wrapper |
| 8 | Towns multi-select | `components/MultiSelect.tsx` | No label association, options had no selected state, Escape did nothing, and the filter input relied on its placeholder as a label | `htmlFor`/`id`, a name that includes the selection, `aria-pressed` options, Escape returns focus to the trigger, `aria-label="Filter towns"` |
| 9 | Workplace suggestions | `components/PlaceInput.tsx` | Arrow-key highlight was invisible to screen readers | `role="combobox"` + `listbox`/`option` + `aria-activedescendant` |

**Warnings / Advisories (not fixed)**

- Warning: the native checkboxes on signup (13×16) and bookmarks (16×16) are under 24px. They fall under the
  user-agent exemption and their labels are clickable, so they pass. A larger custom checkbox would still be kinder on touch.
- Warning: field hints and errors are not linked to their inputs with `aria-describedby`. Errors are still
  announced through `role="alert"`, so this is not a failure.
- Advisory: compare selections live only in memory, so a reload of `/compare` shows the empty state.
- Advisory: map markers are mouse-only. The ranked list offers the same actions by keyboard, so this is not a failure.

### 2. Interface (better-interface, Accessibility rows removed)

| # | Severity | Domain | Location | Before | After | Status |
|---|---|---|---|---|---|---|
| 1 | MEDIUM | Layout | `pages/SearchPrioritiesScreen.tsx` | Search button sat inside the left card, before step 4. On phones the button came before the priorities | Button row comes after step 4 in the DOM. On desktop the grid puts it back under the left card | Fixed |
| 2 | MEDIUM | Writing | `pages/SearchPrioritiesScreen.tsx` | Steps 1 to 3 use numbered badges, but step 4 was plain "4." text | Step 4 uses the same `Step` badge and heading | Fixed |
| 3 | MEDIUM | Typography | `components/ui.tsx` `inputClass`, MultiSelect filter | 14px input text makes iOS Safari zoom the page on focus | `text-base sm:text-sm` | Fixed |
| 4 | MEDIUM | UI polish | `src/index.css` dark layer | In dark mode the "Price is not in the score" callout stayed light yellow, and slider tracks stayed #e3e6ec | Dark surfaces for both (shares the root cause with Blocker 2) | Fixed |
| 5 | LOW | UI polish | `components/ui.tsx` Button/Chip | `active:scale-[0.98]` / `[0.97]` | `active:scale-[0.96]` | Fixed |
| 6 | LOW | UI polish | `LivabilitySliders.tsx:40`, `ScoreRing.tsx:39`, `FlatDetailScreen.tsx:59` | `transition-all` | `transition-[width]` | Fixed |
| 7 | LOW | UI polish | `src/index.css` | Score-bar transitions ignored `prefers-reduced-motion` | Transitions are near-instant under reduced motion. Spinners keep turning | Fixed |

Considered but rejected: making the 50/50 bar legend larger (it is secondary to the bar it labels); moving the
desktop Search button into the right column (the left column is where the required input lives); replacing card
rings with shadows (the `shadow-card` + `ring-ink-900/5` token is consistent across the app).

### 3. Motion (find-animation-opportunities)

Run after the Blockers were fixed. These are suggestions only and are not implemented.

| # | Location | Today | Purpose | Frequency | Suggested motion |
|---|---|---|---|---|---|
| 1 | `pages/ResultsMapListScreen.tsx` ranked list after a re-weight | Cards jump to their new order with no bridge. The product promises "watch the ranking move" | Explanation / spatial consistency | Occasional (once per priority change) | FLIP only the cards in view: `transform: translateY(Δ)` → `0`, `260ms cubic-bezier(0.23, 1, 0.32, 1)`. Under reduced motion, a 150ms opacity crossfade instead |
| 2 | `components/Toasts.tsx` | Toasts appear and vanish instantly | Preventing a jarring change | Occasional | `@starting-style { opacity: 0; translate: 0 8px }` → settled, `transition: opacity, translate 200ms cubic-bezier(0.23, 1, 0.32, 1)`. Exit on the same edge in 150ms. Opacity only under reduced motion |
| 3 | `ResultsMapListScreen.tsx:242` Priorities/Filters panel, `FlatDetailScreen.tsx:452` CPF panel | Panels snap open | State indication | Occasional | `@starting-style { opacity: 0; translate: 0 -4px }`, `180ms cubic-bezier(0.23, 1, 0.32, 1)`. No height animation, because these panels hold inputs |

Rejected:
- Score bars and rings (`ScoreStack`, `ScoreRing`): **rejected on Function.** They already transition, and they are data the user is reading.
- Hover lift on rank cards: **rejected on Frequency.** It fires tens of times a minute, and the existing `motion-safe:hover:-translate-y-0.5` is enough.
- Map marker hover/selection scale: **rejected on Frequency.** It already scales 1.25 with a 150ms transition, so anything more slows list↔map scanning.
- Phone flat sheet (`ResultsMapListScreen.tsx:390`): **rejected on Speed/Function.** Users step through it with prev/next, so a slide per step would make browsing feel slow.

Verdict: the app needs little more motion than it has. Row 1 has the highest leverage, because it makes the
app's core promise visible.

### Not verified / Not run

- Real screen readers (NVDA, VoiceOver) were not run. Name, role and state were checked through the accessibility
  tree and axe, not by listening.
- 200% zoom (1.4.4) and the text-spacing override (1.4.12) were not run separately. The 320px reflow check covers most of the same risk.
- Live government APIs were not reached, because the backend ran in offline mode with sample data.
- Forced-colours / Windows high-contrast mode was not checked.
