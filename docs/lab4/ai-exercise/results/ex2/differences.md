# Differences between the code and the Lab 3 class diagram

Sources: `/home/claude/ai-exp2/lab3-diagram/class-diagram-part1.png` (search, commute, livability; UC-01, UC-02, UC-03, UC-10) and `/home/claude/ai-exp2/lab3-diagram/class-diagram-part2.png` (flat details, valuation, CPF, batch job; UC-04 to UC-07, UC-12). Code: `/home/claude/ai-exp2/backend/src/main/java/com/abcde/hdbresale`.

## Part 1 (search, commute, livability)

1. **SearchSessionStore stereotype.** The diagram shows `«container»`. The code has no such stereotype; the class is a plain `@Component`.
2. **Owner of the anchors association.** The diagram draws `SearchSessionStore 1 -- 1..2 Workplace` ("anchors of each stored search"). In the code, `List<Workplace> anchors` belongs to the nested record `SearchSessionStore.StoredSearch`, not to the store. The store holds `Map<String, StoredSearch>`.
3. **Missing associations from FlatSearchService.** The diagram omits `FlatSearchService` → `ValuationService` (it calls `labelFor` for each ranked flat), `SearchCriteriaRepository` (saves the last search for signed-in users) and `LivabilityWeightService`'s repository. The diagram does show the link to `LivabilityWeightService`.
4. **Missing public method on FlatSearchService.** `findSearch(searchId): Optional<StoredSearch>` is public and not in the diagram. The diagram's `view`, `requireSearch` and `search` signatures match.
5. **CommuteCalculationService internals.** The code also has a 16-thread `ExecutorService pool` and a `@PreDestroy shutdown()`. Neither is in the diagram. `forFlat` returns `null` (not `Optional`) when an anchor cannot be reached.
6. **LivabilityDataService methods.** The code has public `crimeRate(town: String): double` and `density(town: String): double`. Neither is in the diagram. The three repositories it reads (crime, density, air quality) are also not shown.
7. **LivabilityIndex.** The code has a public method `factor(LivabilityFactor): double`. The diagram does not show it.
8. **CommuteScore.Leg.** The diagram shows `legs: List<Leg>` without defining `Leg`. The code defines `Leg(anchorIndex, anchorLabel, mode, minutes, score)`.
9. **WeightProfile.** The code also has `id`, `updatedAt`, `get(factor)`, `set(factor, value)` and `copyFrom(other)`. The diagram shows none of these.
10. **ExternalApiGateway.** The diagram shows only the `«Facade»` interface. The code has `CachingApiGatewayProxy` (`@Primary`, proxy pattern, timeouts and last-known-good values) wrapping `ApiGatewayImpl`. The proxy is not in the diagram.
11. **CommuteRoute.** A value record `CommuteRoute` exists in the domain package. I found no reference to it in the classes I read, so it is not used. The diagram does not show it.

## Part 2 (flat details, valuation, CPF, batch job)

12. **FlatDetailController.getFlatDetail signature.** The diagram shows `getFlatDetail(id, searchId, weights): FlatDetailDto`. The code takes `id`, `searchId` and four separate `Double` query parameters (`lease`, `safety`, `density`, `airQuality`), and builds the weights object itself.
13. **CpfController.submitFinancialProfile return type.** The diagram shows `submitFinancialProfile(): FinancialProfileDto`. The code returns `ResponseEntity<FinancialProfileDto>` and answers 204 No Content when no profile is saved. The method is a GET (`/api/cpf/profile`), and its name suggests a submission.
14. **CpfGrantService public methods.** `estimate`, `savedProfile`, `saveProfile` and `deleteProfile` match. The code also has `savedProfileUpdatedAt(userId): Optional<Instant>`, which the diagram omits.
15. **PostalSectorDirectory.** The diagram has no such class. CpfGrantService depends on it for the parental postal code (`locate`, `areaName`, `isPostalCode`), so it is a missing control-level dependency.
16. **GrantRules attributes.** The diagram shows `ehg`, `phg`, `loan` and `msrPercent`. The code also has `effectiveFrom`, `source` and `cpfHousingGrant` (the Family and Singles grant ceilings and amounts). `cpfHousingGrant` is what `CpfGrantService.cpfHousingGrant` uses, and the diagram omits it.
17. **CpfGrantService dependencies.** The diagram does not show `FlatRepository`, `FinancialProfileRepository` or `PostalSectorDirectory`. The code's `FinancialProfile` entity also has `id` and `updatedAt`, which the diagram does not show.
18. **ValuationService.loadBaselines.** `loadBaselines()` is public and is not in the diagram.
19. **ValuationRefreshScheduler arrow label.** The diagram labels the arrow "every 24 h". The code runs `scheduledRefresh()` on the cron expression `${app.valuation.refresh-cron}` in Asia/Singapore time (daily at a low-traffic hour). `onStartup()` runs a refresh only if the last successful one is more than 24 hours old.
20. **ValuationStrategy.fairBand().** The interface also declares `fairBand(): double`. The diagram shows only `classify`. `ThresholdValuationStrategy` implements `fairBand()` too, and the diagram omits it there as well.
21. **ValuationBaseline attributes.** The diagram shows `averagePrice`, `sampleSize`, `windowStart` and `windowEnd`. The code's fair-value calculation uses `averagePsm: double` (average price per square metre), which the diagram omits. The code also has `computedAt: Instant` and `update(...)`, neither of which is in the diagram.
22. **ValuationBatchJob attributes.** The diagram shows `startedAt`, `finishedAt`, `status`, `recordsIngested` and `finish(...)`. The code also has `triggerReason` and `message`, which the diagram omits.
23. **FlatDetailService dependencies.** The diagram shows only the `FlatSearchService` link. The code also depends on `BookmarkService`, `CommuteCalculationService`, `SchoolRepository` and `TransactionRepository`.

Matches (no difference): `LivabilityController` (all four methods), `SearchController` (both methods), `FlatSearchService.search/view/requireSearch`, `CommuteCalculationService.calculate/forFlat/scoreFor`, `LivabilityWeightService.rebalance/normalise/forUser/saveForUser`, `WeightedSumScoringStrategy`, `LivabilityFactors`, `Workplace`, `CommuteScore`, `CpfController.calculateGrant`, `CpfGrant`, `FinancialProfile` attributes, `ValuationService.labelFor/fairValueFor/valuationFor/refreshBaselines/runBatch`, `ValuationRefreshScheduler.scheduledRefresh/onStartup`, `ThresholdValuationStrategy.classify`, `ValuationLabel` values.
