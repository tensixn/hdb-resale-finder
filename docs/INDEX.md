# Team ABCDE: SC2006 deliverables index

HDB Resale Flat Search and Analysis System (HomeMatch SG). Start here.

| Deliverable | Where | Lab |
|---|---|---|
| How to build, run and test the app | [README.md](../README.md) | 4, 5 |
| Source code: backend (Spring Boot, Java 17) | [backend/src/main](../backend/src/main/java/com/abcde/hdbresale) | 4, 5 |
| Source code: frontend (React, TypeScript) | [frontend/src](../frontend/src) | 4, 5 |
| Automated test cases (68 tests) | [backend/src/test](../backend/src/test/java/com/abcde/hdbresale) | 4, 5 |
| Test design and results (black-box, basis path) | [lab4/Lab4-Test-Report.md](lab4/Lab4-Test-Report.md) ([PDF](lab4/Lab4-Test-Report.pdf)) | 4 |
| Software Requirements Specification v1.4, incl. design model | [requirements/SRS_SC2006_TeamABCDE_v1.4.pdf](requirements/SRS_SC2006_TeamABCDE_v1.4.pdf) | 2, 3, 5 |
| Use case model v1.2 (diagram and descriptions) | [requirements/UseCase_SC2006_TeamABCDE_v1.2.pdf](requirements/UseCase_SC2006_TeamABCDE_v1.2.pdf) | 2, 3, 5 |
| Class diagrams of key classes (PlantUML sources + images) | [design/class-diagrams/](design/class-diagrams/) | 3, 5 |
| UI dialog map | [design/dialog-map/](design/dialog-map/) | 3, 5 |
| Presentation script (15 minutes + 5 minutes Q&A) | [lab5/Presentation-Script.md](lab5/Presentation-Script.md) ([PDF](lab5/Presentation-Script.pdf)) | 5 |
| Demo video shot list (under 7 minutes, captions) | [lab5/Video-Shot-List.md](lab5/Video-Shot-List.md) ([PDF](lab5/Video-Shot-List.pdf)) | 5 |
| Demo video (6:58, captions, no voice-over) | [Google Drive](https://drive.google.com/file/d/1JVu4N5hpKK8hACpdvfAoZ6DmXKBWMnUI/view?usp=sharing) | 5 |
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
| SRS and use case model | Roen, Aleesha, Manwen, Jing Yi, Junyao |
| Class diagrams and dialog map | Roen, Aleesha, Manwen, Jing Yi, Junyao |
| Backend | Roen, Aleesha, Manwen, Jing Yi, Junyao |
| Frontend | Roen, Aleesha, Manwen, Jing Yi, Junyao |
| Tests and test report | Roen, Aleesha, Manwen, Jing Yi, Junyao |
| Demo script and video | Roen, Aleesha, Manwen, Jing Yi, Junyao |

