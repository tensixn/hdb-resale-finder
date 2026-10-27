---
title: "Lab 4 section 3.3: Coding Agent Exercises (kit and reflection template)"
subtitle: "Team ABCDE"
---

The reflections in 3.3.4 must describe what **your team** actually did and saw, so this kit does not contain a
finished reflection. It gives you the setup, prompts and a way to measure the result, so the exercises take about an
hour, and a template to write up afterwards.

## Exercise 1 (3.3.2): regenerate the control class from its tests

The class is `CpfGrantService`, the same control class used for black-box testing in 3.2.1.

### Setup (10 minutes)

1. Install **Cline** in VS Code or IntelliJ and pick a free model (see the Cline docs; do not pay for one).
2. Create a branch so nothing is lost: `git checkout -b ai-exercise-cpf`.
3. Run the baseline so you have a "before" number:
   ```bash
   cd backend && mvn test -Dtest='CpfGrantService*'
   ```
   Expect 35 tests, all passing.
4. **Delete** `backend/src/main/java/com/abcde/hdbresale/service/CpfGrantService.java`. The project will no longer
   compile; that is the point.

### Attempts

Do the attempts in order and stop when the tests pass. Save the agent's output after each attempt (copy the file to
`attempt-N.java`) and fill in the log below.

**Attempt 1: tests and design only.** Mention these files in the prompt so the agent reads them:
`CpfGrantServiceBlackBoxTest.java`, `CpfGrantServiceTest.java`, `GrantRules.java`, `grant-rules.yml`,
`CpfGrantRequestDto.java`, `CpfGrantResponseDto.java`, `FinancialProfileDto.java`, `CpfController.java`,
`PostalSectorDirectory.java`, `docs/lab4/Lab4-Test-Report.md` (section 2), plus your Lab 3 class and sequence diagrams for UC-06.

> Prompt: "The control class `com.abcde.hdbresale.service.CpfGrantService` has been deleted. Recreate it so the project
> compiles and every test in `CpfGrantServiceBlackBoxTest` and `CpfGrantServiceTest` passes. It implements UC-06
> (estimate the CPF Housing Grant, EHG and PHG, down payment, loan, monthly instalment and affordability for one flat) and UC-07 (save,
> read and delete the user's financial profile). Use the rules in `grant-rules.yml` through `GrantRules`; do not hard-code
> amounts. Read the files I listed before writing code. Do not change any test."

**Attempt 2 (only if 1 fails): add the failing output.** Paste the `mvn test` failure summary into the same chat:

> "These tests still fail: [paste]. Fix the class without changing the tests."

Allow up to 3 rounds of this. Count each round.

**Attempt 3 (only if 2 fails): give it the skeleton.** Copy `docs/lab4/ai-exercise/CpfGrantService.skeleton.java`
into place as `CpfGrantService.java`. It has every method signature and Javadoc with the bodies replaced by
`throw new UnsupportedOperationException`. With the skeleton alone, 10 of the 35 tests already pass, because they only
check input validation on the DTOs, so judge the result on the other 25.

> "Complete every method body in `CpfGrantService.java`. Keep the signatures and Javadoc. [same file list and rules as
> attempt 1]"

### What to measure after each attempt

```bash
cd backend && mvn test -Dtest='CpfGrantService*'      # how many of the 35 pass
mvn test                                                # did anything else break (68 total)
git diff --stat main -- src/main                        # did it touch files it should not have
```

Also look at the code, not only the test count. Things worth noting in the reflection:

- Did it hard-code grant amounts instead of reading `GrantRules` (tests pass, design is wrong)?
- Did it keep the 25% minimum down payment notice, the "approximate distance" disclaimer and the rule that the
  parents' postal code is never sent to an external API (NFR-6)?
- Did it try to edit the tests to make them pass?
- How does its structure compare with the original (`git diff main -- backend/src/main/java/com/abcde/hdbresale/service/CpfGrantService.java`)?

### Log (fill in while you work)

| Attempt | Model / tool | Files given | Prompt summary | Tests passing (of 35) | Time | Notes |
|---|---|---|---|---|---|---|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |

When finished: `git checkout main` (or keep the AI version if it is better and the team agrees).

## Exercise 2 (3.3.3): one further experiment

Pick one. We recommend **option A** because Lab 5 needs updated class diagrams anyway, so the work counts twice.

**A. Generate the updated class diagrams for Lab 5.**

> "Read every class under `backend/src/main/java/com/abcde/hdbresale`. Produce a PlantUML class diagram for the
> boundary, control and entity classes of UC-06 and UC-01, with stereotypes «boundary», «control», «entity», the key
> attributes and public methods, and associations with multiplicities. Then list every place where the code differs
> from the attached Lab 3 diagram."

Attach your Lab 3 class diagram. Render the output at plantuml.com and check it against the code by hand: look for
invented classes or methods, missing associations and wrong multiplicities.

**B. Code quality review and refactor.**

> "Review `FlatSearchService` for readability, duplication and design problems. List the 3 changes with the best
> value for the risk, then make the first one. All 68 tests must still pass."

**C. Implement a missing feature.** For example: "Add a CSV export of the compare table (UC-09) with a backend endpoint and a
button on the compare screen, with a test."

Record the same things as in Exercise 1: prompt, files given, rounds, outcome, and whether tests still pass.

## Reflection template (3.3.4, about half a page each)

Write one for each exercise. Cover every bullet; the marker looks for them.

**Exercise [1 / 2]: [title]**

- **Tool and model:** e.g. Cline 3.x with [model], free tier.
- **Which experiment (Exercise 2 only):** option A, B or C and why the group chose it.
- **How we prompted and what we provided:** files mentioned, diagrams attached, the prompt (short quote or summary).
- **Iteration:** how many rounds, what we added each round (failure output, skeleton, hints).
- **Outcome:** tests passing per attempt; anything wrong that the tests did not catch; how it compared with our code.
- **Did it meet our expectations?** Better or worse than expected, and why.
- **If it failed, what could be done differently:** smaller tasks, a skeleton, more context, a stronger model, splitting
  the class, writing more tests first.
- **Author(s):** _names_
