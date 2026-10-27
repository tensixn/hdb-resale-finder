---
title: "Lab 5: Final Demo Script (20 minutes) and Video Plan"
subtitle: "HDB Resale Flat Search and Analysis System (HomeMatch SG), Team ABCDE"
---

| Work product | Author(s) |
|---|---|
| Demo script | _fill in_ |
| Slides (SE practices, traceability) | _fill in_ |
| Demo video | _fill in_ |

This replaces the Lab 4 demo script. Lab 5 gives each team about **20 minutes**: 13 to 15 minutes of live demo, 2 to 3
minutes on SE practices and design, and 2 to 3 minutes showing traceability for one or two use cases (manual 3.1.4 to
3.1.6). The video is now **5 to 7 minutes** (manual 3.3).

## 1. The pitch in one line

> "Most flat portals ask *where do you want to live?* We ask *where do you both work?* and then tell you which flats
> are fair value and what you can actually afford."

Three things the client should remember, in this order:

1. **Dual-anchor search.** Ranks flats by commute to *two* workplaces plus a Livability Index the buyer can re-weight.
2. **Fair-value label on every flat.** Under, Fair or Over-Valued against the estate's last 6 months of sales.
3. **CPF grants (Family Grant, EHG, PHG) and instalment in one panel**, with the buyer's income encrypted and never sent to a third party.

## 2. Roles

The manual allows team members to play different actors (3.1.3), so the roles below double as actors.

| Role | Who | Does |
|---|---|---|
| Presenter | _name_ | Use case overview, tells the story, keeps time |
| Driver / Home Buyer | _name_ | Only person touching the demo laptop; plays Wei Ling and Arjun |
| Scheduler / System | _name_ | Shows the UC-12 batch refresh (4.9) |
| Engineer | _name_ | SE practices and design (4.10) and traceability (4.11); takes technical questions |
| Backup | _name_ | Second laptop on the same build, video cued up; holds up time cards at 5, 10, 13 and 18 minutes |

## 3. Before the demo (day before and 30 minutes before)

- [ ] Build once: `./build-jar.sh`, then run `java -jar backend/target/hdbresale.jar` and open http://localhost:8080.
      The app runs **fully offline** (H2 + bundled sample data), so venue Wi-Fi cannot break it.
- [ ] Start with a fresh database: delete `backend/data/` (or the `DATA_DIR` folder) so sign-up works with the demo email.
- [ ] **Re-check the numbers in the click script the day before.** Offline sample prices shift slightly with the date
      and the 6-month valuation window moves each month, so ranks and labels may differ from those captured on
      27 Oct 2026. Update the "say" lines if they do.
- [ ] Browser at 125% zoom, bookmarks bar hidden, notifications off. Phone view ready in DevTools (iPhone 14 Pro).
- [ ] Second window: the slides (use case diagram, architecture, class diagram Part 2, test summary).
- [ ] IDE open on `CpfGrantService.java` and `CpfGrantServiceBlackBoxTest.java`, with the test run configuration ready
      (or a terminal in `backend/` with `mvn test -Dtest=CpfGrantServiceBlackBoxTest` typed in).
- [ ] Backup laptop running the same jar; demo video downloaded locally (not streamed).
- [ ] Confirm the CPF Housing Grant amounts on hdb.gov.sg (the EHG and PHG amounts were checked on 27 Oct 2026).

## 4. Running order (20 minutes)

| Time | Section | Who |
|---|---|---|
| 0:00 to 1:30 | 4.1 Use case overview | Presenter |
| 1:30 to 12:15 | 4.2 to 4.8 Live demo as the Home Buyer | Presenter + Driver |
| 12:15 to 13:30 | 4.9 Live demo as the Scheduler (UC-12) | Scheduler |
| 13:30 to 16:00 | 4.10 SE practices, design and future upgrades | Engineer |
| 16:00 to 18:30 | 4.11 Traceability: UC-06 end to end | Engineer |
| 18:30 to 20:00 | 4.12 Close and questions | Presenter |

Personas:

- **Wei Ling and Arjun** (Home Buyers, engaged). Wei Ling works at **Raffles Place**, Arjun at **one-north**.
  Budget up to **$650,000** for a **4-room**. Combined income **$8,500**. Wei Ling's parents live in Bishan
  (postal code **570123**). They start as **guests**, then sign up.
- **The Scheduler** (system actor): the nightly valuation refresh.

### 4.1 Use case overview (0:00 to 1:30)

Slide: the use case diagram (three actors, fifteen use cases).

> "Three actors use the system. The **Home Buyer** is the main one, as a guest or a registered user. The **Scheduler**
> refreshes fair-value baselines every night. And the government **Data APIs** (OneMap, LTA DataMall, data.gov.sg)
> supply addresses, travel times, resale prices and air quality. Guests can search, re-weight, filter, view details,
> check fair value and estimate grants (UC-01 to UC-07, UC-10). Bookmarks, comparison and the profile need an account
> (UC-08, UC-09, UC-11, UC-13 to UC-15). Everything you'll see in the next eleven minutes is one of these use cases."

### 4.2 Problem and search (1:30 to 3:30) — UC-01, UC-02

| Driver does | Presenter says |
|---|---|
| Landing page already open | "Couples in Singapore usually work in different places. Picking a flat means trading off two commutes, the estate, the price and the grants. Today that is five tabs and a spreadsheet." |
| Type `Raffles Place` and `one-north`, tick Public transport, max price 650,000, flat type 4 ROOM | "Wei Ling and Arjun just type where they work. Postal codes work too." |
| Click Search | "We pull every matching flat, compute door-to-door travel time to both offices, and rank them in well under 3 seconds." (27 Oct: 189 flats) |

### 4.3 Results, map and re-weighting (3:30 to 5:30) — UC-03, UC-10

| Driver does | Presenter says |
|---|---|
| Point at the map pins | "Pins are numbered by rank, and hovering a card highlights its pin, so you can see where the good options cluster." |
| Point at the grey line under the #1 card | "Every rank explains itself. Number 1 leads number 2 by 1.6 points mainly because of a quicker trip to one-north, and it tells you what it gives up: it is more crowded." (27 Oct: #1 was 688B Henderson Rd, Bukit Merah, 23 minutes to each office) |
| Point at **The short version** box | "If you only read one thing: best overall, shortest commute, lowest price and most livable, one tap each." |
| Open Priorities and click the **Safe & sound** preset | "Safety matters most to them. One tap sets safety to 55% and the other three share the rest; you can still fine-tune each slider, and the weights always add up to 100%." |
| Results re-rank | "Bishan jumps to the top, and each card that moved says why: *Up 1* because safety now counts for more." (27 Oct: 340 Bishan St 12 to #1) |
| Apply a town filter, then clear it | "Filters narrow the list without running the search again." |

### 4.4 Flat detail and fair value (5:30 to 7:15) — UC-04, UC-05

| Driver does | Presenter says |
|---|---|
| Open Blk 340 Bishan St 12 | "*Our verdict* sums it up in one line: one of the best matches, and priced below fair value. Below it the Livability Index is broken into its four factors, with commute legs, nearby schools and the sales history." |
| Scroll to Fair-value pricing | "This is the part buyers ask about most. The last sale was $603,000. Similar 4-room flats in Bishan sold for about $6,768 per square metre over the last 6 months, so a unit this size should be about $663,000. That is more than 5% below, so it is **Under-Valued**." |
| Point at the sample size and baseline date | "We show how many sales the estimate uses (7 here) and when it was computed. With fewer than 3 sales we say *No estimate* rather than guess." |

### 4.5 CPF grants and affordability (7:15 to 8:45) — UC-06, UC-07

| Driver does | Presenter says |
|---|---|
| Open *CPF grants & affordability*, enter 8,500 / Engaged / parents 570123, click *Estimate my grants* | "They enter combined income and where Wei Ling's parents live." |
| Results appear | "They qualify for three grants: the Family Grant for a 4-room resale flat, the Enhanced Housing Grant, and the Proximity Housing Grant because her parents are about 2 km away. That is $110,000 off the loan. The monthly instalment is about $1,550, about 18% of income, so *Comfortable* against the 30% limit." (27 Oct: Family Grant $80,000 + EHG $10,000 + PHG $20,000, $1,553 a month) |
| Open the loan assumptions and drag tenure to 20 years | "Every loan assumption is visible and adjustable, and the instalment updates as you drag." |
| (Say, do not click) | "Income and the parents' postal code are encrypted in our database, and the postal code is never sent to an outside map service. Grant rules sit in one config file so they can be updated when HDB changes them." **If the amounts are not yet verified, add:** "These grant figures are from our reading of HDB's 2024 tables and would be verified before launch." |

### 4.6 Registered buyer: sign up, bookmark, compare (8:45 to 10:45) — UC-13, UC-08, UC-09, UC-11

| Driver does | Presenter says |
|---|---|
| Click the bookmark star as a guest | "Guests can do everything we've shown. Saving needs an account." |
| Sign up (`demo@abcde.sg`, `Demo1234`), tick consent | "Sign-up takes one screen and asks for consent under the PDPA. After sign-up you land back where you were and the bookmark is saved." |
| Bookmark two more flats (one Bishan, one Queenstown) | |
| Open Compare | "Side by side: price, price per square metre, fair value, commutes and livability factors. The best flat on each measure is marked with a crown." |

### 4.7 Profile, sign out, lockout (10:45 to 11:45) — UC-14, UC-15, UC-11

| Driver does | Presenter says |
|---|---|
| Open Profile | "Saved weights, last search and financial profile, which they can delete at any time." |
| Sign out, then sign in with a wrong password 5 times | "After 5 wrong attempts the account locks for 15 minutes, and the message never says whether it was the email or the password that was wrong." |

### 4.8 Phone layout (11:45 to 12:15)

| Driver does | Presenter says |
|---|---|
| Toggle DevTools device view | "Same app on a phone: list first, map one tap away." |

### 4.9 The Scheduler: nightly refresh (12:15 to 13:30) — UC-12

| Scheduler does | Says |
|---|---|
| Open Help, point at *Data status* | "Every night at 3 a.m. the Scheduler pulls new resale transactions from data.gov.sg and recomputes the fair-value baselines. Here is the last run." |
| Click **Refresh now** | "This runs the same job on demand. It skips towns whose records are incomplete instead of computing a baseline from bad data, and if data.gov.sg is down it keeps yesterday's baseline, logs an alert, and the detail page says the estimate may be out of date." |

### 4.10 SE practices, design and future upgrades (13:30 to 16:00) — Engineer

Three or four slides. About 30 seconds per point.

1. **Layered design that matches the class diagram.** Boundary (React screens, REST controllers), control
   (`FlatSearchService`, `CpfGrantService`, `ValuationService`, ...), entity (JPA). Class names in the code are the ones
   in our design model, so the diagrams and the code read the same.
2. **Patterns that make upgrades cheap.**
   - *Facade + Proxy*: one `ExternalApiGateway` interface hides OneMap, LTA and data.gov.sg; `CachingApiGatewayProxy`
     caches travel times and cuts off any call slower than 2 seconds, falling back to the last value (NFR-3).
     **Upgrade:** a new data provider is one new client class; no service changes.
   - *Strategy*: `WeightedSumScoringStrategy` and `ThresholdValuationStrategy`. **Upgrade:** a machine-learning
     valuation model is a new `ValuationStrategy`; search, detail and compare don't change.
   - *Configuration over code*: grant amounts and income ceilings live in `grant-rules.yml`. **Upgrade:** when HDB
     changes the grants (as it did on 24 Aug 2026) only that file changes.
3. **Security and privacy by design.** bcrypt passwords, JWT backed by server-side sessions (30-minute idle, 8-hour
   cap), lockout after 5 failures, login rate limit, AES-256-GCM for income and the parents' postal code, which is
   never sent to an outside service.
4. **Testing.** 68 automated tests: black-box (equivalence classes and boundary values) on `CpfGrantService`, basis
   path on `login()` (V(G) = 5) and the batch `ingest()` (V(G) = 7), and end-to-end API flows. Four deliberately
   planted bugs were all caught.
5. **Runs two ways.** Laptop mode (H2, offline, nothing to install) and production mode (`docker compose up` with
   PostgreSQL + PostGIS and Redis).

### 4.11 Traceability: UC-06 Calculate CPF Grant and Affordability (16:00 to 18:30) — Engineer

Walk one thread from requirement to test. Have each artefact open in its own tab or window before the demo.

| Step | Show | Say |
|---|---|---|
| 1. Requirement | SRS: UC-06 and FR-11 to FR-13 | "UC-06 says the buyer enters a financial profile and sees grants, loan and instalment. FR-11 to FR-13 are the testable requirements behind it." |
| 2. Design | Class diagram Part 2 (`CpfController` → `CpfGrantService` → `GrantRules`, `CpfGrant`, `FinancialProfile`) | "One boundary, one control class, rules kept in configuration, and the profile as an encrypted entity." |
| 3. UI | Dialog map: Flat detail → CPF panel | "In the UI it's a panel inside the flat detail screen, so the buyer never loses their place." |
| 4. Code | IDE: `CpfGrantService.estimate()`, then `cpfHousingGrant()`, `ehg()`, `phg()` | "Each grant is its own method, each reading its numbers from `GrantRules`." |
| 5. Test | IDE: run `CpfGrantServiceBlackBoxTest`; 30 tests go green | "We partitioned every input into equivalence classes and tested the boundaries: S$9,000 versus S$9,001 for the EHG, 4.0 versus 4.01 km for the PHG, 25% versus 24.99% down payment. The test report maps each test to its requirement." |

If there is time, repeat steps 4 and 5 for UC-11 (`login()` and its five basis paths).

### 4.12 Close and questions (18:30 to 20:00)

> "Two commutes, a livability score you control, an honest price check and every grant you qualify for, in one search,
> built so that new data sources, valuation models and grant rules drop in without rewriting it."

Leave the results page on screen for questions.

### If something goes wrong

| Problem | Do this |
|---|---|
| App will not start / crashes | Backup laptop takes over; Driver keeps talking through the same step |
| A step behaves unexpectedly | Skip to the next section; do not debug live |
| Running late at 11:00 | Skip 4.7 and 4.8 |
| Total failure | Play the demo video from section 6 and keep the engineering section live |


## 5. Mapping to the Lab 5 manual

| The manual asks for | Where |
|---|---|
| Use case diagram overview, expected users (3.1.4) | 4.1 |
| All features of the working product (3.1.4) | 4.2 to 4.9 |
| Different actors played by team members (3.1.3) | Home Buyer (Driver), Scheduler (4.9) |
| SE practices, system design, envisioned upgrades (3.1.5) | 4.10 |
| Traceability for one or two use cases, class diagram, implementation, testing (3.1.6) | 4.11 |
| Additional features that beat competitors (2.3) | Rank explanations, priority presets, "The short version", fair-value verdict, three grants in one panel, offline fallback |

## 6. Demo video plan (5 to 7 minutes)

Record at 1920x1080 with OBS Studio (free) or the built-in macOS / Windows recorder. Record each scene separately and join
them, so a mistake only costs one scene. Add the voice-over to the finished cut. Upload to YouTube (unlisted) if the file is
too big for GitHub, and put the link in the index file.

| Time | Scene | On screen | Voice-over (about 130 words a minute) |
|---|---|---|---|
| 0:00 to 0:30 | Title and problem | App name, team, one-line pitch | "Finding a resale flat when you and your partner work in different places means trading off two commutes, the estate, the price and the grants. We built one search that handles all of it." |
| 0:30 to 0:55 | Users | Use case diagram | "Home Buyers use it as guests or with an account; a nightly Scheduler keeps fair-value data fresh; government APIs supply the data." |
| 0:55 to 1:45 | Search and results | Enter both workplaces, budget, flat type; results and map | "Enter both workplaces and a budget. Every matching flat is ranked by travel time to both offices and by a Livability Index, and each rank explains why it beats the next one." |
| 1:45 to 2:20 | Priorities | Safe & sound preset; list re-ranks with *Up 1* reasons | "Change what matters and the ranking updates instantly, telling you which flats moved and why." |
| 2:20 to 3:05 | Detail and fair value | Verdict, factor breakdown, fair-value bar | "Each flat is checked against six months of sales of the same flat type in the same town. This one is about $60,000 under its fair value." |
| 3:05 to 3:50 | CPF | Enter profile; three grants, loan, instalment | "Enter income and where your parents live to see every grant you qualify for, your loan and monthly instalment against the 30% limit. Your income is encrypted and never leaves our system." |
| 3:50 to 4:30 | Account features | Sign up from a bookmark; compare; profile | "Sign up to save flats and compare them side by side; the best flat on each measure is marked." |
| 4:30 to 4:50 | Phone | Device view | "It works just as well on a phone." |
| 4:50 to 5:15 | Scheduler | Help page, Refresh now | "Every night the Scheduler refreshes the fair-value baselines and falls back safely if the data source is down." |
| 5:15 to 6:15 | Design and testing | Architecture slide, class diagram Part 2, IDE test run going green | "A layered Spring Boot and React design that follows our class diagrams, with a facade and caching proxy for the government APIs, swappable strategies for scoring and valuation, and 68 automated tests including black-box and basis path suites." |
| 6:15 to 6:30 | Close | Title card | "HomeMatch SG, by Team ABCDE." |

Unlike the live demo, the video should be perfect: re-record any scene with a typo or a wait longer than 2 seconds.
