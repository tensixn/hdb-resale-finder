# HDB Resale Finder

SC2006 Software Engineering project by Team ABCDE.

A web app that helps buyers search HDB resale flats, rank them by commute and livability, see if the asking price is fair, and estimate CPF housing grants.

## Stack

- Backend: Spring Boot 3.3, Java 17, Maven (package `com.abcde.hdbresale`)
- Frontend: React + TypeScript, Vite, Tailwind CSS, MapLibre
- Database: H2 for local runs, PostgreSQL + PostGIS planned
- External data: OneMap, LTA DataMall, data.gov.sg

## Status

This is the application skeleton we submitted for Lab 3. It follows the class diagram in our Design Model report.

What works now:

- Domain entities, DTOs, repositories, database schema (Flyway) and config are in place.
- UC-03 Adjust Livability Weights is implemented: `LivabilityWeightService` and `WeightedSumScoringStrategy`, with unit tests.
- The backend compiles and all 9 tests pass.

Not done yet (Lab 4 work):

- Most services are declarations with TODOs (search, commute, valuation, CPF, bookmarks, compare).
- API caching and fallback, and full sign in and sign up.
- The frontend has page and component files only. It does not have `index.html` or a Vite config yet, so it does not build.

## Build and run the backend

You need Java 17 and Maven 3.9.

```bash
cd backend
mvn test              # 9 tests: 8 for UC-03, 1 that starts the whole app
mvn spring-boot:run   # API on http://localhost:8080, uses an embedded H2 database
```

Try UC-03:

```bash
curl -X POST localhost:8080/api/livability/weights/adjust -H 'Content-Type: application/json' \
  -d '{"factor":"SAFETY","value":40,"current":{"lease":25,"safety":25,"density":25,"airQuality":25}}'
```

## Repo layout

```
backend/    Spring Boot API
frontend/   React app (skeleton files)
```

## Traceability

| Use case | Control class | REST endpoint |
|---|---|---|
| UC-01 Search Flats, UC-10 Filter | FlatSearchService | POST /api/search, POST /api/search/{id}/view |
| UC-02 Calculate Commute Time | CommuteCalculationService | (internal) |
| UC-03 Adjust Livability Weights | LivabilityWeightService | /api/livability/weights |
| UC-04 View Flat Details | FlatDetailService | GET /api/flats/{id} |
| UC-05 View Fair-Value Pricing | ValuationService | GET /api/flats/{id}/valuation |
| UC-06, UC-07 CPF Grant, Financial Profile | CpfGrantService | POST /api/cpf/estimate, PUT /api/profile/financial |
| UC-08 Bookmark Flat | BookmarkService | /api/bookmarks |
| UC-09 Compare Bookmarked Flats | CompareService | POST /api/compare |
| UC-11, UC-13, UC-15 Sign in, Register, Sign out | AuthenticationService | /api/auth/login, /register, /logout |
| UC-12 Update Valuation Baseline | ValuationRefreshScheduler, ValuationService | (scheduled daily) |
| UC-14 Manage Profile | ProfileService | GET /api/profile |
