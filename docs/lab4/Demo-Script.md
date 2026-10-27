---
title: "Lab 4: Live Demo Script and Video Plan"
subtitle: "HDB Resale Flat Search and Analysis System (HomeMatch SG), Team ABCDE"
---

| Work product | Author(s) |
|---|---|
| Demo script | Roen, Aleesha, Manwen, Jing Yi, Junyao |
| Video | Roen, Aleesha, Manwen, Jing Yi, Junyao |

## 1. The pitch in one line

> "Most flat portals ask *where do you want to live?* We ask *where do you both work?* and then tell you which flats
> are fair value and what you can actually afford."

Three things the client should remember, in this order:

1. **Dual-anchor search.** Ranks flats by commute to *two* workplaces plus a Livability Index the buyer can re-weight.
2. **Fair-value label on every flat.** Under, Fair or Over-Valued against the estate's last 6 months of sales.
3. **CPF grants (Family Grant, EHG, PHG) and instalment in one panel**, with the buyer's income encrypted and never sent to a third party.

## 2. Roles

| Role | Who | Does |
|---|---|---|
| Presenter | _name_ | Talks to the client, tells the story, keeps time |
| Driver | _name_ | Only person touching the laptop; follows the click script below |
| Engineer | _name_ | Presents section 5 (architecture, patterns, testing) and takes technical questions |
| Backup | _name_ | Second laptop running the same build, video cued up, watches the clock (cards at 5, 10, 13 min) |

## 3. Before the demo (day before and 30 minutes before)

- [ ] Build once: `./build-jar.sh`, then run `java -jar backend/target/hdbresale.jar` and open http://localhost:8080.
      The app runs **fully offline** (H2 + bundled sample data), so venue Wi-Fi cannot break it.
- [ ] Start with a fresh database: delete `backend/data/` (or the `DATA_DIR` folder) so sign-up works with the demo email.
- [ ] **Re-check the numbers in the click script the day before.** Offline sample prices shift slightly with the date and
      the 6-month valuation window moves each month, so ranks and labels may differ from those captured on 27 Oct 2026.
      Update the "say" lines if they do.
- [ ] Browser at 125% zoom, bookmarks bar hidden, notifications off, one tab only. Phone view ready in DevTools
      (iPhone 14 Pro preset).
- [ ] Backup laptop running the same jar; 5-minute video downloaded locally (not streamed).
- [ ] Check grant amounts in `grant-rules.yml` against hdb.gov.sg. If the team has not verified them, say so when
      showing the CPF panel (see 4.4). Do not present unverified figures as official.

## 4. Click script (15 minutes)

Personas, so the client sees different end-users:

- **Wei Ling and Arjun** (Home Buyers, engaged). Wei Ling works at **Raffles Place**, Arjun at **one-north**.
  Budget up to **$650,000** for a **4-room**. Combined income **$8,500**. Wei Ling's parents live in Bishan
  (postal code **570123**). They start as **guests**, then sign up.
- **The system itself** (scheduler / administrator view): the nightly valuation refresh and failure handling.

### 4.1 Problem and search (0:00 to 2:30) — UC-01, UC-02

| Driver does | Presenter says |
|---|---|
| Landing page already open | "Couples in Singapore usually work in different places. Picking a flat means trading off two commutes, the estate, the price and the grants. Today that is five tabs and a spreadsheet." |
| Type `Raffles Place` and `one-north`, tick Public transport, max price 650,000, flat type 4 ROOM | "Wei Ling and Arjun just type where they work. Postal codes work too." |
| Click Search | "We pull every matching flat, compute door-to-door travel time to both offices, and rank them in well under 3 seconds." (27 Oct: 189 flats) |

### 4.2 Results, map and re-weighting (2:30 to 4:30) — UC-03, UC-10

| Driver does | Presenter says |
|---|---|
| Point at the map pins | "Pins are numbered by rank, and hovering a card highlights its pin, so you can see where the good options cluster." |
| Point at the grey line under the #1 card | "Every rank explains itself. Number 1 leads number 2 by 1.6 points mainly because of a quicker trip to one-north, and it tells you what it gives up: it is more crowded." (27 Oct: #1 was 688B Henderson Rd, Bukit Merah, 23 minutes to each office) |
| Point at **The short version** box | "If you only read one thing: best overall, shortest commute, lowest price and most livable, one tap each." |
| Open Priorities and click the **Safe & sound** preset | "Safety matters most to them. One tap sets safety to 55% and the other three share the rest; you can still fine-tune each slider, and the weights always add up to 100%." |
| Results re-rank | "Bishan jumps to the top, and each card that moved says why: *Up 1* because safety now counts for more." (27 Oct: 340 Bishan St 12 to #1) |
| Apply a town filter, then clear it | "Filters narrow the list without running the search again." |

### 4.3 Flat detail and fair value (4:30 to 6:30) — UC-04, UC-05

| Driver does | Presenter says |
|---|---|
| Open Blk 340 Bishan St 12 | "*Our verdict* sums it up in one line: one of the best matches, and priced below fair value. Below it the Livability Index is broken into its four factors, with commute legs, nearby schools and the sales history." |
| Scroll to Fair-value pricing | "This is the part buyers ask about most. The last sale was $603,000. Similar 4-room flats in Bishan sold for about $6,768 per square metre over the last 6 months, so a unit this size should be about $663,000. That is more than 5% below, so it is **Under-Valued**." |
| Point at the sample size and baseline date | "We show how many sales the estimate uses (7 here) and when it was computed. With fewer than 3 sales we say *No estimate* rather than guess." |

### 4.4 CPF grants and affordability (6:30 to 8:00) — UC-06, UC-07

| Driver does | Presenter says |
|---|---|
| Open *CPF grants & affordability*, enter 8,500 / Engaged / parents 570123, click *Estimate my grants* | "They enter combined income and where Wei Ling's parents live." |
| Results appear | "They qualify for three grants: the Family Grant for a 4-room resale flat, the Enhanced Housing Grant, and the Proximity Housing Grant because her parents are about 2 km away. That is $110,000 off the loan. The monthly instalment is about $1,550, about 18% of income, so *Comfortable* against the 30% limit." (27 Oct: Family Grant $80,000 + EHG $10,000 + PHG $20,000, $1,553 a month) |
| Open the loan assumptions and drag tenure to 20 years | "Every loan assumption is visible and adjustable, and the instalment updates as you drag." |
| (Say, do not click) | "Income and the parents' postal code are encrypted in our database, and the postal code is never sent to an outside map service. Grant rules sit in one config file so they can be updated when HDB changes them." **If the amounts are not yet verified, add:** "These grant figures are from our reading of HDB's 2024 tables and would be verified before launch." |

### 4.5 Registered buyer: sign up, bookmark, compare (8:00 to 10:00) — UC-13, UC-08, UC-09, UC-11

| Driver does | Presenter says |
|---|---|
| Click the bookmark star as a guest | "Guests can do everything we've shown. Saving needs an account." |
| Sign up (`demo@abcde.sg`, `Demo1234`), tick consent | "Sign-up takes one screen and asks for consent under the PDPA. After sign-up you land back where you were and the bookmark is saved." |
| Bookmark two more flats (one Bishan, one Queenstown) | |
| Open Compare | "Side by side: price, price per square metre, fair value, commutes and livability factors. The best flat on each measure is marked with a crown." |

### 4.6 Profile, sign out, lockout (10:00 to 11:00) — UC-14, UC-15, UC-11

| Driver does | Presenter says |
|---|---|
| Open Profile | "Saved weights, last search and financial profile, which they can delete at any time." |
| Sign out, then sign in with a wrong password 5 times | "After 5 wrong attempts the account locks for 15 minutes, and the message never says whether it was the email or the password that was wrong." |

### 4.7 Phone layout (11:00 to 11:30)

| Driver does | Presenter says |
|---|---|
| Toggle DevTools device view | "Same app on a phone: list first, map one tap away." |

### 4.8 Engineering (11:30 to 14:00) — Engineer presents

Use 3 or 4 slides or the README diagram. Keep each point to about 30 seconds.

1. **Layered design that matches our Lab 3 class diagram.** Boundary (React screens, REST controllers), control
   (`FlatSearchService`, `CpfGrantService`, `ValuationService`, ...), entity (JPA). Class names are the ones in our design.
2. **Patterns where they earn their place.**
   - *Proxy*: `CachingApiGatewayProxy` caches OneMap travel times and cuts off any call slower than 2 seconds,
     falling back to the last known value and showing "may be outdated" (NFR-3).
   - *Strategy*: `WeightedSumScoringStrategy` and `ThresholdValuationStrategy` can be swapped without touching search or
     detail code.
   - *Gateway*: one `ExternalApiGateway` interface hides OneMap, LTA DataMall and data.gov.sg, so a provider can be
     replaced.
3. **The system as an end-user (UC-12).** Click *Refresh now* under Data status on the Help page (or describe the 3 a.m. job):
   it pulls new sales, skips incomplete towns, recomputes baselines, and on failure keeps yesterday's baseline and logs an alert.
4. **Security and privacy.** bcrypt passwords, JWT with server-side sessions (30-min idle, 8-hour cap), account
   lockout, login rate limit, AES-256-GCM for income and parental postal code.
5. **Testing.** 68 automated tests. Black-box (equivalence classes and boundaries) on `CpfGrantService`; basis path
   on `login()` (V(G) = 5) and the batch `ingest()` (V(G) = 7). We injected 4 deliberate bugs and the tests caught all 4.
6. **Runs two ways.** Laptop mode (H2, offline, nothing to install) and production mode (`docker compose up`:
   PostgreSQL + PostGIS + Redis).

### 4.9 Close (14:00 to 15:00)

> "Two commutes, a livability score you control, an honest price check and the grants you qualify for, in one search.
> Next we'd connect live HDB listings when they become available, add school-distance balloting rules, and
> push alerts when an under-valued flat appears near both offices."

Leave the results page on screen for questions.

### If something goes wrong

| Problem | Do this |
|---|---|
| App will not start / crashes | Backup laptop takes over; Driver keeps talking through the same step |
| A step behaves unexpectedly | Skip to the next section; do not debug live |
| Running late at 10:00 | Skip 4.6 and 4.7 |
| Total failure | Play the 5-minute video from section 6 and keep the engineering section live |

## 5. Mapping to the marking criteria

| The client wants to see | Where |
|---|---|
| Specified functionality, different end-users | 4.1 to 4.7 (guest buyer, registered buyer), 4.8 item 3 (system / scheduler) |
| Innovative solutions | Dual-anchor ranking, adjustable Livability Index, fair-value label, grants + instalment in one panel, offline fallback |
| Good software engineering | 4.8: layered design traceable to Lab 3, Proxy / Strategy / Gateway, security, test techniques with evidence |

## 6. Five-minute video plan

Record at 1920x1080 with OBS Studio (free) or the macOS / Windows built-in recorder. Record each scene separately, then join
them, so a mistake only costs one scene. Voice-over is recorded over the finished cut.

| Time | Scene | On screen | Voice-over (about 130 words a minute) |
|---|---|---|---|
| 0:00 to 0:25 | Title | App name, team, one-line pitch | "Finding a resale flat when you and your partner work in different places means trading off two commutes, the estate, the price and the grants. We built one search that handles all of it." |
| 0:25 to 1:10 | Search and results | Enter both workplaces, budget, flat type; results and map appear | "Enter both workplaces and a budget. Every matching flat is ranked by travel time to both offices and by a Livability Index. Pins are coloured by livability." |
| 1:10 to 1:45 | Re-weight | Drag Safety up; list re-ranks | "Change what matters and the ranking updates instantly. The weights always add up to 100%." |
| 1:45 to 2:30 | Detail and fair value | Detail page, factor breakdown, fair value box | "Each flat is checked against six months of sales of the same flat type in the same town. This one is under-valued by about $60,000." |
| 2:30 to 3:15 | CPF | Enter profile, show grants and instalment | "Enter income and where your parents live to see your grants, loan and monthly instalment against the 30% limit. Your income is encrypted and never leaves our system." |
| 3:15 to 3:50 | Account features | Sign up from a bookmark, compare screen, profile | "Sign up to save flats and compare them side by side." |
| 3:50 to 4:10 | Phone | Device view | "It works just as well on a phone." |
| 4:10 to 4:50 | Engineering | Architecture diagram, test run in terminal (`mvn test`, green summary) | "Under the hood: a layered Spring Boot and React design that follows our class diagram, a caching proxy that keeps the app working when government APIs are slow, and 68 automated tests including black-box and basis path suites." |
| 4:50 to 5:00 | Close | Title card | "HomeMatch SG, by Team ABCDE." |

Unlike the live demo, the video should be perfect: re-record any scene with a typo or a wait longer than 2 seconds.
