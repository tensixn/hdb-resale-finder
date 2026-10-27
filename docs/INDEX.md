# Team ABCDE: SC2006 deliverables index

HDB Resale Flat Search and Analysis System (HomeMatch SG). Start here.

| Deliverable | Where | Lab |
|---|---|---|
| How to build, run and test the app | [README.md](../README.md) | 4, 5 |
| Source code: backend (Spring Boot, Java 17) | [backend/src/main](../backend/src/main/java/com/abcde/hdbresale) | 4, 5 |
| Source code: frontend (React, TypeScript) | [frontend/src](../frontend/src) | 4, 5 |
| Automated test cases (68 tests) | [backend/src/test](../backend/src/test/java/com/abcde/hdbresale) | 4, 5 |
| Test design and results (black-box, basis path) | [lab4/Lab4-Test-Report.md](lab4/Lab4-Test-Report.md) ([PDF](lab4/Lab4-Test-Report.pdf)) | 4 |
| Software Requirements Specification, incl. design model | [requirements/](requirements/) (`SRS_SC2006_TeamABCDE_v1.x.pdf`) | 2, 3, 5 |
| Use case model (diagram and descriptions) | [requirements/](requirements/) (`UseCase_SC2006_TeamABCDE_v1.x.pdf`) | 2, 3, 5 |
| Class diagrams of key classes (PlantUML sources + images) | [design/class-diagrams/](design/class-diagrams/) | 3, 5 |
| UI dialog map | [design/dialog-map/](design/dialog-map/) | 3, 5 |
| Demo script (final, 20 minutes) and video plan | [lab5/Demo-Script.md](lab5/Demo-Script.md) | 5 |
| Demo video (5 to 7 minutes) | _add YouTube link here_ | 5 |
| Coding-agent exercises and reflections | [lab4/ai-exercise/](lab4/ai-exercise/) | 4 |
| Meeting minutes | [minutes/](minutes/) | all |
| How the UI design was chosen | [design-arena.md](design-arena.md) | 4 |

## Class diagrams

| Part | Use cases | Image | Source |
|---|---|---|---|
| 1 Search, commute and livability | UC-01, UC-02, UC-03, UC-10 | [PNG](design/class-diagrams/part1-search-livability.png) | [puml](design/class-diagrams/part1-search-livability.puml) |
| 2 Flat details, valuation, CPF, batch job | UC-04 to UC-07, UC-12 | [PNG](design/class-diagrams/part2-detail-valuation-cpf.png) | [puml](design/class-diagrams/part2-detail-valuation-cpf.puml) |
| 3 Accounts, bookmarks, comparison, profile | UC-08, UC-09, UC-11, UC-13 to UC-15 | [PNG](design/class-diagrams/part3-accounts-bookmarks.png) | [puml](design/class-diagrams/part3-accounts-bookmarks.puml) |
| 4 Start-up and external integration | all | [PNG](design/class-diagrams/part4-integration.png) | [puml](design/class-diagrams/part4-integration.puml) |
| 5 Persistent entities and multiplicities | all | [PNG](design/class-diagrams/part5-entities.png) | [puml](design/class-diagrams/part5-entities.puml) |

To re-render after a code change: `java -jar plantuml.jar -tpng docs/design/class-diagrams/*.puml` (no Graphviz needed;
the sources use PlantUML's built-in layout).

## Authors

| Work product | Author(s) |
|---|---|
| SRS and use case model | _fill in_ |
| Class diagrams and dialog map | _fill in_ |
| Backend | _fill in_ |
| Frontend | _fill in_ |
| Tests and test report | _fill in_ |
| Demo script and video | _fill in_ |

Team: Roen, Aleesha, Manwen, Jing Yi, Junyao.
