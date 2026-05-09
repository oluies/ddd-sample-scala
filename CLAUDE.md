# CLAUDE.md — ddd-sample-scala

This file briefs Claude Code on the repository. Keep it short; rely on the
skills in `.claude/skills/` and the plan in `.claude/plans/` for depth.

## What this is

A Scala port of the **Citerus DDD Sample** — the canonical Eric Evans cargo
shipping example used to teach Domain-Driven Design tactical patterns. The
Java original lives at <https://github.com/citerus/dddsample-core>; this
repo demonstrates the same model in Scala.

This is a **teaching artifact**, not a production system. Optimize for
clarity over cleverness. Don't introduce frameworks or abstractions that
obscure the DDD lesson.

## Current state

- **Scala 3.3.4 LTS / Java 17 / sbt 1.10.** `sbt compile`, `sbt Test/compile`,
  and `sbt test` are all green on `task007/scala3-15d0bf` (6 unit tests pass).
- Test stack: ScalaTest 3.2.19 + scalatestplus-mockito + ScalaCheck 1.18.
- Spring 5.2.19 + Hibernate 5.4 + CXF 3.3 — Java side stays on the
  `javax.*` era libs (Java 17 is the highest baseline that runs them clean).
- HSQLDB in-memory database for integration tests.
- See `README.md` "Migration notes (2026-05)" for the migration outcome and
  `.claude/plans/scala3-upgrade.md` for the original phased plan.

**Known follow-ups** (don't surprise the next session):
- `interfaces.tracking.CargoTrackingController` is a stub — original
  extended Spring 2.x's removed `SimpleFormController`. Needs port to
  Spring 5 `@Controller` / `@GetMapping`.
- `infrastructure.persistence.hibernate.AbstractRepositoryTest` and
  `CargoRepositoryTest` are stubs — original used Spring 2.x test
  framework. Needs port to Spring 5 `SpringExtension` / `@SpringJUnitConfig`.
- `-Wunused:imports`, `-Wvalue-discard`, `-Xfatal-warnings` are commented
  out in `build.sbt`. Re-enable in a cleanup PR after fixing the unused
  imports left by the migration.
- Test coverage is thin (6 tests). Value objects, aggregate logic, the
  `Specification` composers, and `HandlingEventFactory`'s exception cases
  all lack tests. See "Tests worth adding" below.

## Tests worth adding

The migration didn't aim to grow coverage — it aimed to keep what was
there compiling and passing. These would be high-value additions:

- **Value objects:** `TrackingId`, `UnLocode`, `VoyageNumber` — equality
  contract, validation rules, round-trip through `idString`. Good
  property-test candidates with ScalaCheck.
- **Specifications:** `AbstractSpecification` + `And/Or/NotSpecification`
  composition behavior (truth tables).
- **Itinerary:** `isExpected(event)` for each `HandlingEventType`,
  `finalArrivalLocation`/`Date`, `lastLeg` edge cases.
- **Cargo:** `assignToRoute` updates delivery synchronously,
  `specifyNewRoute` invariants, `deriveDeliveryProgress`.
- **Delivery:** `transportStatus` / `routingStatus` / `misdirected` /
  `nextExpectedActivity` for each event-type permutation. This is the
  most logic-heavy class; deserves a focused suite.
- **HandlingHistory:** ordering by completion time,
  `mostRecentlyCompletedEvent` with empty / single / many events.
- **HandlingEventFactory:** `UnknownCargoException`,
  `UnknownVoyageException`, `UnknownLocationException` paths.

## Layout

```
src/main/scala/se/citerus/dddsample/
  domain/
    model/{cargo,handling,location,voyage}/   ← aggregates
    service/                                  ← domain services
    shared/                                   ← Entity, ValueObject, Specification
  application/                                ← use-case orchestration (no domain logic)
  infrastructure/persistence/hibernate/       ← repository implementations
  infrastructure/routing/                     ← external pathfinder integration
  interfaces/{booking,handling,tracking}/     ← Spring MVC + CXF facades

src/main/scala/com/pathfinder/                ← in-process pathfinder service
src/main/scala/com/aggregator/                ← inbound handling aggregator (CXF)
src/main/webapp/                              ← JSP, web.xml, Spring config
src/test/scala/se/citerus/dddsample/          ← mirrors src/main/scala layout
```

## Conventions

- **DDD discipline.** `domain.*` never imports from `infrastructure.*` or
  `interfaces.*`. Application services orchestrate; aggregates own
  invariants. See `.claude/skills/scala-ddd-tactical/SKILL.md`.
- **Tests mirror source packages**, one suite per production class.
- **Hibernate mappings** are XML, not annotations. Domain classes stay
  framework-free.
- **Integration tests** load a real Spring context against HSQLDB. Don't
  replace them with mocks — they catch ORM-mapping regressions.
- **Skills:**
  - `.claude/skills/scala3-migration/SKILL.md` — invoke when editing Scala
    sources during the migration.
  - `.claude/skills/scala-ddd-tactical/SKILL.md` — invoke when adding to
    or refactoring the domain model.
  - `.claude/skills/scala-testing/SKILL.md` — invoke when writing or
    porting tests.

## Common commands

```bash
sbt compile                       # main compile
sbt test                          # all tests
sbt "testOnly *CargoTest"         # one suite
sbt scalafmtAll                   # format
sbt scalafmtCheckAll              # CI-style format check
sbt "Jetty/start"                 # web UI on :8080 (xsbt-web-plugin)
sbt package                       # WAR
sbt bookingFacadeJar              # secondary JAR for JAX-WS facade
```

Java 17 + sbt 1.10 are required. `brew install sbt` (and `brew install
openjdk@17` if needed).

## Dependency hygiene

- `.github/dependabot.yml` — weekly sbt + GitHub Actions bumps, grouped by
  family (Spring, CXF, Hibernate, ActiveMQ, Jackson, logging, test
  frameworks). Defers `org.scala-lang*` to Scala Steward.
- `.scala-steward.conf` — repo-specific Scala Steward config. Enroll the
  repo in the relevant `scala-steward-repos` list.

## What NOT to do

- Don't add Cats Effect / ZIO / Akka to the existing bounded contexts.
  The sample is deliberately framework-light. New experimental contexts
  may use them, but isolate them.
- Don't replace XML Hibernate mappings with annotations as a side effect
  of unrelated work — that's a separate, riskier migration.
- Don't `--no-verify` past pre-commit hooks. If a hook fails, fix the cause.
- Don't re-enable `-Xfatal-warnings` in `build.sbt` without first cleaning
  up unused imports — it'll break the build.
