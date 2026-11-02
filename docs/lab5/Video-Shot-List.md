---
title: "Lab 5 demo video: shot list and captions"
subtitle: "HomeMatch SG, Team ABCDE. Target length 6:30 (limit: under 7:00). No voice-over, just on-screen captions."
---

**Rules from the team notes:** start at the authentication page, cover every feature we have (as listed in the SRS),
stay under 7 minutes, and use no voice-over. Captions tell the viewer what is happening. Show only what the app
actually does. Section 3 lists the SRS features that are built only partly.

## 1. Recording setup

- Run `java -jar backend/target/hdbresale.jar`. **Delete `backend/data/` first** so sign-up works with a fresh email.
- Use a 1920×1080 screen recording at 110 to 125% browser zoom. Hide the bookmarks bar and turn off notifications.
- Record each shot as its own clip. Re-takes are cheap, and you can trim the waiting time between shots.
- Captions: two lines at most, bottom centre, on screen for at least 3 seconds. Put the **FR / UC tag** in small text
  on the right so the marker can tick off requirements.
- Personas: **Wei Ling and Arjun** (engaged). Wei Ling works at Raffles Place, Arjun at one-north. Budget $650,000,
  4-room, income $8,500, Wei Ling's parents at 570123.
- The numbers below come from the real-data snapshot (data.gov.sg, Dec 2025 to Oct 2026). Delete `backend/data` before you record.

## 2. Shot list

| # | Time | On screen (what to do) | Caption | Tag |
|---|---|---|---|---|
| 1 | 0:00 to 0:10 | Title card: "HomeMatch SG, Team ABCDE", the five names, "SC2006 Lab 5" | "HomeMatch SG: find an HDB resale flat that works for *both* your commutes." | |
| 2 | 0:10 to 0:35 | Sign-in page. Click **Sign up**, enter `demo@abcde.sg` / `Demo1234` twice, tick PDPA consent, Submit | "New users sign up with email, password and consent to the personal data notice." | UC-13, FR-27 |
| 3 | 0:35 to 0:50 | Lands signed in; point the cursor at the account name in the nav bar | "Signed in. The session is time-limited and secured with a token." | UC-11, FR-25 |
| 4 | 0:50 to 1:05 | Search screen: click **Search** with Workplace 1 empty, so the error appears | "Workplace 1 is required. The search won't run without it." | FR-19 |
| 5 | 1:05 to 1:30 | Type `Raffles Place` and `one-north`, tick **Public transport**, set max price 650,000, Search | "Enter two workplaces and how you travel. Every matching flat is ranked by commute to both." | UC-01, FR-1, FR-2, FR-3, FR-4 |
| 6 | 1:30 to 1:55 | Results: list on the left, map on the right; hover a card so its pin highlights | "Ranked list and map. Pins are coloured by Livability Index and numbered by rank." | FR-17, FR-23, FR-31 |
| 7 | 1:55 to 2:10 | Zoom in on the explanation line under card #1 and **The short version** box | "Every rank explains itself: why it beats the next flat, and what it gives up." | FR-31 |
| 8 | 2:10 to 2:35 | Priorities: click **Long lease**, then drag the Safety slider | "Re-weight what matters. The other weights adjust so the total stays 100%." | UC-03, FR-5, FR-6 |
| 9 | 2:35 to 2:45 | Cards re-rank, showing "Up 1" reasons | "The Livability Index is recomputed and the list re-ranks instantly." | FR-7, FR-8 |
| 10 | 2:45 to 3:05 | Filters: min price 400,000, flat type 4 ROOM, town Bishan, Apply; then Clear | "Filter by price range, flat type and town without searching again." | UC-10, FR-18, FR-22 |
| 11 | 3:05 to 3:20 | Set max price to 1,000 and Apply, so "No matching flats" appears with suggestions; then Clear | "No matches? The app tells you which filter to relax." | FR-20 |
| 12 | 3:20 to 3:45 | Open Blk 123 Bishan St 12: verdict, livability breakdown, commute legs, schools, price history | "Flat details: verdict, livability breakdown, both commutes, schools and past sales." | UC-04 |
| 13 | 3:45 to 4:10 | Scroll to Fair-value pricing | "Real sale: $603,888 (Sep 2026). From 16 similar recent sales, a fair price is about $663,000, so this flat is Under-Valued." | UC-05, FR-9, FR-32 |
| 14 | 4:10 to 4:45 | CPF panel: 8,500, Engaged, parents 570123, tick **Save to my profile**, Estimate | "Enter income, marital status and parents' postal code. Saved encrypted only if you choose." | UC-06, UC-07, FR-10 |
| 15 | 4:45 to 5:05 | Results: Family Grant $80k, EHG $10k, PHG $20k; instalment about $1,556, Comfortable | "Three grants worth $110,000, the monthly instalment, and an affordability check against the 30% limit." | FR-11, FR-12 |
| 16 | 5:05 to 5:20 | Open loan assumptions and drag tenure to 20 years | "Adjust tenure, down payment and rate; the instalment updates." | FR-13 |
| 17 | 5:20 to 5:40 | Click the bookmark star on this flat and on a Queenstown flat; open Bookmarks; remove one, then add it back | "Bookmark flats, or remove them." | UC-08, FR-14, FR-15 |
| 18 | 5:40 to 6:00 | Select both, then **Compare** | "Compare bookmarked flats side by side. The best on each measure is marked." | UC-09, FR-16 |
| 19 | 6:00 to 6:15 | Profile: saved weights, financial profile; click **Delete financial profile** | "Your profile: saved weights and financial details, which you can update or delete for good." | UC-14, FR-21, FR-33, FR-34 |
| 20 | 6:15 to 6:25 | Help, then Data status, then **Refresh now**; it shows SUCCESS (0 new) and the latest sales month 2026-10 | "Each night the app checks for new sales and recomputes fair value. If the source fails, it keeps the last good data." | UC-12, FR-28, FR-29, FR-30 |
| 21 | 6:25 to 6:35 | Profile, then **Sign out**, which returns to Search as a guest | "Sign out ends the session on the server." | UC-15, FR-35 |
| 22 | 6:35 to 6:50 | Login: wrong password 5 times, then the lock message | "After 5 wrong attempts the account locks for 15 minutes." | FR-24, FR-26 |
| 23 | 6:50 to 6:55 | End card: GitHub repo URL and team names | "Thank you." | |

Total: about 6:55. **If you run over 7:00**, cut shot 11 (no matches) and shorten shot 12, which saves about 25
seconds.

Recording note for shot 22: the lockout lasts 15 minutes. Record it last, or use a second account.

## 3. SRS features check: what's built (checked against the app code on 1 Nov)

Every FR from FR-1 to FR-35 has working code and appears in the shot list above. Be careful what the captions
claim for these three:

| FR | SRS says | What the app actually does | What to do in the video |
|---|---|---|---|
| FR-3 | Travel times **via the OneMap and LTA DataMall APIs** | The client code for both APIs exists and is used in `live` mode with API keys. The default (`offline`) mode, used for the demo, **estimates** travel times with a built-in model. Live mode has **never been run against the real APIs** with real keys. | Don't caption "live from OneMap". Use the shot 5 caption as written. If anyone has free OneMap and LTA keys, try `EXTERNAL_MODE=live` once before the video. |
| FR-28 | **Queries data.gov.sg** every 24 hours | The scheduler and the failure handling (FR-29, FR-30) are built and tested. In offline mode, "Refresh now" runs the same job but adds no sales (0 new), because the data is a fixed snapshot. | Use the shot 20 caption as written. Don't say "downloaded just now". |
| Data | (implied) real resale transactions | **Real** since 1 Nov: 1,488 flats and 1,738 sales from data.gov.sg (Dec 2025 to Oct 2026), located with OneMap (`tools/fetch_real_seed.py`). Travel times, crime, density and schools are still estimates. | End card can say: "Real HDB resale data from data.gov.sg; travel times estimated." |

Everything else matches the SRS, including the map pins coloured by Livability Index (FR-17), the min and max price
filter (FR-22) and comparing two or more flats (FR-16).

Caveat: this check used the project-files copy of the app. Main on GitHub has since merged PRs #3 to #5 (CI and
fixes). Those changes shouldn't remove features, but record from a fresh build of **main**.
