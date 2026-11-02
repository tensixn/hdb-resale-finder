---
title: "Lab 5 presentation script (15 minutes + Q&A)"
subtitle: "HomeMatch SG, Team ABCDE: Roen, Aleesha, Manwen, Jing Yi, Junyao"
---

**Format (team notes, confirmed by Roen on 1 Nov):** 5 minutes on the idea, then 10 minutes showing the app, then
5 minutes of Q&A. The tutor cuts you off at **15:00** if you overrun, so this script finishes the demo by **14:00**,
leaving a minute of buffer.

Who presents is flexible and the order is random, so every section has a "say" line anyone can read. Everyone must
be on time.

**Skipped on purpose:** sign-up and login (the team agreed to skip them for time), lockout, profile and phone layout.
All of these are in the demo video.

**Manual check:** the Lab 5 manual (3.1.6) asks for 2 to 3 minutes on traceability, meaning one use case from
requirement to class diagram to code to test. The team's 5/10 split leaves no slot for it, so the 60-second version
is placed at 3:30 to 4:30 below. Drop it only if your tutor said it isn't needed.

## Before you start

- Laptop: `java -jar backend/target/hdbresale.jar`, then open http://localhost:8080 at 125% zoom. Search, prices and
  grants run offline, but the map's street tiles need internet, so bring a hotspot.
- **Sign in before the session** as `demo@abcde.sg` with bookmarks for Blk 123 Bishan St 12 and Blk 10 Holland Ave.
  That way the bookmark star and Compare work without showing login.
- Have the slide deck open in a second window: https://claude.ai/artifact/K8QuR2jiFUayHxNYdhBdDR (slides in order: The problem, Why we picked it, Users, What is new, Traceability, How it is built, Live demo, Q&A, then backup screenshots).
- **The numbers in quotes come from the real-data snapshot** (data.gov.sg sales from Dec 2025 to Oct 2026, checked on 1 Nov).
  They stay the same until someone re-runs `tools/fetch_real_seed.py`. Delete `backend/data` before the demo so the app
  loads the snapshot fresh.
- Have the demo video downloaded on a second laptop in case the live demo fails.

## Part 1: the idea and why we picked it (0:00 to 5:00)

| Time | Slide / screen | Say |
|---|---|---|
| 0:00 to 1:30 | Cover, The problem, Why we picked it | "Most HDB portals ask *where do you want to live?* We ask *where do you both work?* Most couples in Singapore work in two different places. Choosing a resale flat means trading off two commutes, the estate, the price and the grants. Today that means five tabs and a spreadsheet. We picked this problem because it is ours: it is the decision our parents and older siblings are making right now." |
| 1:30 to 2:30 | Users | "Three actors. The Home Buyer, as a guest or registered user. A Scheduler that refreshes prices every night. And the government data APIs: OneMap, LTA DataMall and data.gov.sg." |
| 2:30 to 3:30 | What is new | "What's new compared with existing portals: one, **dual-anchor search** ranked by commute to two workplaces plus a Livability Index you can re-weight. Two, **every rank explains itself**. Three, a **fair-value label** on every flat. Four, **all three CPF grants and the monthly instalment** in one panel, with your income encrypted." |
| 3:30 to 4:30 | Traceability in 60 seconds | Traceability in 60 seconds: "Take UC-06, the grant calculator. FR-11 in our SRS names the three grants. In the class diagram, CpfController calls CpfGrantService, which reads the amounts from one config file. 28 black-box test cases (BB-01 to BB-28) cover it, written from the SRS, with boundaries at every income ceiling. When HDB changed the grants, we edited one file and the tests told us what moved." |
| 4:30 to 5:00 | How it is built | "It's Spring Boot and React, split into boundary, control and entity classes, the same layers as in our class diagrams. All 68 automated tests run every time we push to GitHub. Let's show you." |

## Part 2: live demo of the novelty features (5:00 to 14:00)

Persona: **Wei Ling and Arjun**, engaged. Wei Ling works at Raffles Place and Arjun at one-north. Budget $650,000
for a 4-room, combined income $8,500. Wei Ling's parents live at postal code 570123 (Bishan).

| Time | Driver does | Say |
|---|---|---|
| 5:00 to 6:30 | Search: type `Raffles Place` and `one-north`, tick Public transport, set max $650,000 and 4 ROOM, then click Search | "They type where they work. Postal codes work too. We compute travel time from every matching flat to both offices and rank them." (342 flats) |
| 6:30 to 7:30 | Point at the map pins, then the line under card #1 | "Pins are coloured by livability and numbered by rank. Every rank explains itself: #1 beats #2 mainly on the trip to one-north, and it tells you what it gives up." (#1: Blk 123 Bishan St 12, $603,888) |
| 7:30 to 8:00 | Point at **The short version** | "If you read one thing: best overall, shortest commute, lowest price and most livable, one tap each." |
| 8:00 to 9:15 | Priorities, then the **Long lease** preset, then drag one slider | "They plan to stay for decades, so lease matters most. One tap sets lease to 55%, and the other three share the rest, always totalling 100%. The list re-ranks instantly, and each card that moved says why." (Blk 10 Holland Ave, Queenstown rises from #4 to #2; Blk 21 Teban Gdns Rd enters the top 3) |
| 9:15 to 9:45 | Apply the Bishan town filter, then clear it | "Filters narrow the list without searching again." |
| 9:45 to 11:15 | Open Blk 123 Bishan St 12 and scroll to Fair-value pricing | "*Our verdict* in one line. Then the part buyers ask about most. This 84 m² flat sold for $603,888 in September. That's a real record from data.gov.sg. The 16 recent Bishan 4-room sales put a fair price at about $663,000, so it's about 9% below, which is **Under-Valued**. We show how many sales the estimate uses, and if there are fewer than 3 we say *No estimate* instead of guessing." |
| 11:15 to 13:00 | CPF panel: enter 8,500, Engaged, 570123, then Estimate; drag tenure to 20 years | "They qualify for three grants: the Family Grant, the Enhanced Housing Grant, and the Proximity Grant because her parents are about 1.6 km away. That's $110,000 off. The instalment is about $1,556, 18% of income, so Comfortable against the 30% limit. Every loan assumption is adjustable. Income and the parents' postal code are encrypted and never sent to an outside service." |
| 13:00 to 14:00 | Help, then Data status, then **Refresh now** | "Every night the Scheduler checks data.gov.sg for new resale sales and works out fair value again. We're offline today, so it finds nothing new and tells you that. If the source is down, it keeps the last good numbers. That's HomeMatch SG." |

**If you're ahead of time** (before 12:30 when the CPF section ends): open Bookmarks, then Compare, already signed
in. "Saved flats side by side, with the best on each measure marked." Otherwise skip it, since it's in the video.

**If you run long:** drop the town filter first, then shorten the slider step to the preset only.

**If the demo breaks:** switch to the second laptop, or play the video from the CPF section onward.

## Part 3: Q&A (15:00 to 20:00, likely questions)

| Question | Short answer |
|---|---|
| Is this real data? | "Yes for flats and prices: 1,488 real flats and 1,738 real sales from data.gov.sg (Dec 2025 to Oct 2026), placed on the map with OneMap. Travel times are estimated by our own model, and crime and density are approximate town figures. The app has a live mode for OneMap, LTA and data.gov.sg, but we demo with the offline snapshot so the venue Wi-Fi can't break the search." Only say "tested live" if someone has actually run it. |
| How is the match score computed? | "50% commute, 50% Livability Index. The index is a weighted sum of lease, safety, density and air quality, using your weights." |
| How accurate are the grants? | "The amounts come from HDB's published rules effective 24 Aug 2026, kept in one config file. It's an estimate, and HDB makes the final call." |
| How did you test it? | "Black-box tests with equivalence classes and boundary values on the grant calculator, basis path tests on login and the nightly refresh, plus end-to-end API tests. 68 in total, run in CI on every push." |
| Did you use AI? | Answer honestly from your own experience and the Lab 4 AI exercise. |
| What would you build next? | "Live listings instead of past transactions, and a school-distance filter." |
