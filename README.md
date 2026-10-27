# HomeMatch SG: HDB Resale Flat Search and Analysis System

SC2006 Team ABCDE. A working implementation of SRS v1.2 and Use Cases v1.2 (UC-01 to UC-15).

Enter where you and your partner work, and the app ranks HDB resale flats by commute to both places and by a
Livability Index you can re-weight. Each flat shows whether its last sale price was fair for its estate, and
what CPF grants and monthly instalment you could expect.

The UI deliberately does **not** follow the wireframes in the SRS. Every rank explains itself: each result shows
how its 100 match points split between commute and the four livability factors, what helps and hurts it most, and
why it beats or trails its neighbour. Re-weighting shows what moved and why. Results sit beside a map with pins
coloured by Livability Index, and on phones the map is one tap away. The design was chosen from three competing
versions; see `docs/design-arena.md`.

```
hdb-resale-finder/
├── backend/     Spring Boot 3.3 (Java 17) REST API, package com.abcde.hdbresale
├── frontend/    React 18 + TypeScript + Vite + Tailwind + MapLibre
├── tools/       generate_seed.py (rebuilds the sample data CSVs)
├── build-jar.sh one jar that serves API + web app
├── Dockerfile, docker-compose.yml   production-like stack (PostgreSQL/PostGIS + Redis)
```

## 1. Run it on your own computer

No database, Redis or API keys are needed. By default the backend uses an embedded H2 database and runs the
external APIs in offline mode with sample data. You only install **Java 17+** and **Node 20+**; Maven comes with the
project (`mvnw` / `mvnw.cmd` downloads it on first run).

### 1.1 Install Java and Node (once)

| System | Commands |
|---|---|
| Windows (PowerShell) | `winget install EclipseAdoptium.Temurin.17.JDK` then `winget install OpenJS.NodeJS.LTS` |
| macOS ([Homebrew](https://brew.sh)) | `brew install openjdk@17 node` |
| Ubuntu / Debian | `sudo apt update && sudo apt install openjdk-17-jdk nodejs npm` |

Close and reopen the terminal, then check: `java -version` shows 17 or higher and `node -v` shows 20 or higher.

### 1.2 Get the code

```bash
git clone https://github.com/tensixn/hdb-resale-finder.git
cd hdb-resale-finder
```

Or download the zip and unzip it. On Windows, from your Downloads folder:
`Expand-Archive .\homematch-sg.zip -DestinationPath .\homematch-sg` then `cd .\homematch-sg\app`.

### 1.3 Start the backend (terminal 1)

```bash
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

The first run downloads Maven and the libraries, so allow a few minutes. It is ready when the log shows
`Started Application` and `Loaded 650 flats`. **Leave this window open**; closing it stops the backend. The
"Using generated security password" line can be ignored: the app does not use it.

### 1.4 Start the website (terminal 2)

```bash
cd frontend
npm install                     # first time only
npm run dev
```

Open **http://localhost:5173**. Try workplaces like `Raffles Place`, `one-north`, `Jurong East`,
`Changi Business Park` or a 6-digit postal code such as `238859`.

To stop, press `Ctrl+C` in each window. On first start the backend loads 650 flats in 25 towns and about 1,300
resale transactions; they are stored in `backend/data/`. Delete that folder to start fresh.

### 1.5 If something goes wrong

| Symptom | Fix |
|---|---|
| `cd : Cannot find path ...` | You are in the wrong folder. `cd` into the unzipped or cloned folder first (`dir` / `ls` shows `backend` and `frontend`). |
| `java` or `node` is not recognized | Reopen the terminal after installing. On macOS run `brew info openjdk@17` and run the `sudo ln -sfn ...` line it prints. |
| `npm.ps1 cannot be loaded because running scripts is disabled` (Windows) | Run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` once, or use `npm.cmd install` and `npm.cmd run dev`. |
| `Port 8080 was already in use` | Another backend is still running. Close it, or start on another port: `$env:PORT=8081` (Windows) / `PORT=8081` (macOS/Linux), and change `proxy` in `frontend/vite.config.ts` to match. |
| Website loads but searches fail | The backend window is closed or still starting. Wait for `Started Application`. |
| Map shows no streets | The map tiles come from OneMap and need internet access. Flats and search still work offline. |

## 2. Run it as a single app

macOS / Linux:

```bash
./build-jar.sh                       # builds the frontend into the jar
java -jar backend/target/hdbresale.jar
```

Windows (PowerShell, from the project folder):

```powershell
cd frontend; npm ci; npm run build; cd ..
Remove-Item -Recurse -Force backend\src\main\resources\static -ErrorAction SilentlyContinue
Copy-Item -Recurse frontend\dist backend\src\main\resources\static
cd backend; .\mvnw.cmd -DskipTests package; cd ..
java -jar backend\target\hdbresale.jar
```

Open http://localhost:8080.

## 3. Run the production-like stack (Docker)

```bash
docker compose up --build
```

Open http://localhost:8080. This uses the `docker` Spring profile: PostgreSQL 16 with PostGIS for storage and
Redis for the commute and geocoding cache, as in SRS Section 2.4.

## 4. Use the real government APIs

Offline mode is the default so the app always runs. To call OneMap, LTA DataMall and data.gov.sg instead:

On Windows PowerShell use `$env:EXTERNAL_MODE="live"` (and the same for the other variables) instead of `export`.

```bash
export EXTERNAL_MODE=live
export ONEMAP_EMAIL=you@example.com ONEMAP_PASSWORD=...   # free OneMap account, used for search + routing
export LTA_ACCOUNT_KEY=...                                  # free LTA DataMall key
./mvnw spring-boot:run
```

data.gov.sg resale prices and NEA PSI need no key. If any API fails or takes longer than 2 seconds, the app
falls back to cached or offline values and shows a "may be outdated" banner (NFR-3).

## 5. Configuration

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | API port |
| `DATA_DIR` | `./data` | H2 database folder (default profile) |
| `SPRING_PROFILES_ACTIVE` | none | `docker` switches to PostgreSQL + Redis |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` | `localhost` `5432` `hdbresale` `hdb` `hdb` | PostgreSQL (docker profile) |
| `REDIS_HOST` `REDIS_PORT` | `localhost` `6379` | Redis (docker profile) |
| `EXTERNAL_MODE` | `offline` | `live` calls the real APIs |
| `ONEMAP_EMAIL` `ONEMAP_PASSWORD` `LTA_ACCOUNT_KEY` | empty | API credentials for live mode |
| `JWT_SECRET` | dev value | Signs session tokens. **Change outside your own machine.** |
| `ENCRYPTION_KEY` | dev value | Base64 32-byte AES-256 key for income and parental address. **Change outside your own machine.** |

Business rules (TBD values from the SRS) are in `backend/src/main/resources/application.yml` under `app.*`, and
grant amounts are in `grant-rules.yml`.

## 6. Tests

```bash
cd backend && ./mvnw test      # 68 tests: strategies, CPF rules, cache/timeout proxy, full API flows,
                              # plus the Lab 4 black-box and basis path suites
cd frontend && npm test       # helper unit tests
```

`qa/e2e.mjs` walks every use case in a real browser at desktop and phone width (search, list/map sync, re-weight,
presets, filters, keyboard, detail, CPF, sign-up from a bookmark, compare, profile, sign out). It needs Playwright:
`npm i -D playwright && npx playwright install chromium`, then `BASE=http://localhost:5173 node qa/e2e.mjs`.

Lab 4 test design, results, demo script and the coding-agent exercise kit are in `docs/lab4/`.
All SC2006 deliverables (requirements, class diagrams, dialog map, test report, demo script, minutes) are indexed in
[`docs/INDEX.md`](docs/INDEX.md).

## 7. Where each use case lives

| Use case | Screen | API | Main classes |
|---|---|---|---|
| UC-01 Search Flats by Dual-Anchor | Search, Results | `POST /api/search` | `SearchController`, `FlatSearchService` |
| UC-02 Calculate Commute Time | Results, Flat detail | (internal) | `CommuteCalculationService`, `ExternalApiGateway`, `CachingApiGatewayProxy` |
| UC-03 Adjust Livability Weights | Priorities panel | `GET/PUT /api/livability/weights`, `POST …/adjust`, `…/reset`, `POST /api/search/{id}/view` | `LivabilityController`, `LivabilityWeightService`, `WeightedSumScoringStrategy` |
| UC-04 View Flat Details | Flat detail | `GET /api/flats/{id}` | `FlatDetailController`, `FlatDetailService` |
| UC-05 View Fair-Value Pricing | Flat detail | `GET /api/flats/{id}/valuation` | `ValuationService`, `ThresholdValuationStrategy` |
| UC-06 Calculate CPF Grant and Affordability | Flat detail (CPF panel) | `POST /api/cpf/estimate` | `CpfController`, `CpfGrantService` |
| UC-07 Input Financial Profile | CPF panel, Profile | `PUT/DELETE /api/profile/financial` | `ProfileController`, `FinancialProfile` (encrypted) |
| UC-08 Bookmark Flat | Star on any flat, Bookmarks | `/api/bookmarks` | `BookmarkController`, `BookmarkService` |
| UC-09 Compare Bookmarked Flats | Compare | `POST /api/compare` | `CompareController`, `CompareService` |
| UC-10 Filter Search Results | Filters panel | `POST /api/search/{id}/view` | `FlatSearchService` |
| UC-11 Authenticate User | Sign in | `POST /api/auth/login` | `AuthenticationService`, `JwtService`, `LoginRateLimiter` |
| UC-12 Update Valuation Baseline | (scheduled, daily 03:00) | `POST /api/system/valuation-refresh` | `ValuationRefreshScheduler`, `ValuationService` |
| UC-13 Register Account | Sign up | `POST /api/auth/register` | `AuthenticationService` |
| UC-14 Manage Profile | Profile | `/api/profile` | `ProfileService` |
| UC-15 Sign Out | Nav bar (Sign out) | `POST /api/auth/logout` | `AuthenticationService` |

Class names follow the team's Lab 3 skeleton, so the code matches the design model report.

## 8. Rules the app uses

- **Match score** = 50% commute + 50% Livability Index (TBD-2). Commute scores 100 at 15 minutes or less and 0 at
  90 minutes or more, using the fastest of the chosen modes and averaged across workplaces.
- **Livability Index** = weighted sum of remaining lease, safety, space (density) and air quality, 25% each by
  default (TBD-3). Moving one slider rebalances the others so the total stays 100%.
- **Fair value** = average price per m² of the same flat type in the same town over the last 6 months, times the
  flat's floor area. Within ±5% is fair (TBD-1). Fewer than 3 sales gives "No estimate".
- **Sign-in**: 5 failed attempts lock the account for 15 minutes (TBD-4). Sessions expire after 30 idle minutes
  and at most 8 hours (TBD-5).
- **Privacy**: household income and parents' postal code are stored AES-256-GCM encrypted and never logged (NFR-6).

## 9. Known gaps (read before demoing)

- **Grant amounts were checked on 27 Oct 2026.** The EHG (families and singles) and PHG amounts in `grant-rules.yml`
  match HDB's tables. The CPF Housing Grant (Family / Singles Grant) amounts and the S$16,000 / S$8,000 income ceilings
  from 24 Aug 2026 come from secondary sources and should be confirmed on hdb.gov.sg. All grants assume first-time
  Singapore citizen buyers.
- **Sample data is synthetic.** Flats, prices, crime and density figures are generated by
  `tools/generate_seed.py` to look realistic. Live mode replaces resale prices and PSI with real data, but crime
  and density stay as the static tables the SRS assumes (A-5).
- **Live mode has not been tested end to end**, because the build environment could not reach the government APIs.
  The request formats follow each API's public documentation.
- **The Docker image itself has not been built here** (no Docker daemon was available). The same jar was run
  against a real PostgreSQL 16 and Redis 7 with the `docker` profile, and the full journey passed.
- Minimum lease and minimum floor area filters run in the browser over the (at most 500) results, because the
  view API only filters by price, flat type and town.
- PostGIS is installed but distances are computed in Java, not with spatial queries. At 500 flats per search
  this is well under the 3-second target (NFR-1); searches took about 0.3 seconds in testing.
- The brand name "HomeMatch SG" is a placeholder. Search the frontend for `HomeMatch` to rename it
  (`index.html`, `NavBar.tsx`, `SignupScreen.tsx`, `HelpScreen.tsx`).

## 10. Screenshots

See `docs/screenshots/`: results with map, re-weighted results, priority presets, flat detail, CPF panel, and the
phone results, map and detail screens.
