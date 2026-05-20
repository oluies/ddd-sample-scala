# CLAUDE.md — ddd-sample-scala (rewrite/from-java-master)

This file briefs Claude Code on the **rewrite branch**. Keep it short; rely
on the skills in `.claude/skills/` and the plan in `.claude/plans/` for
depth.

## What this is

A Scala port of the **Citerus DDD Sample** — the canonical Eric Evans cargo
shipping example used to teach Domain-Driven Design tactical patterns. The
Java original lives at <https://github.com/citerus/dddsample-core>; this
repo demonstrates the same model in Scala.

This is a **teaching artifact**, not a production system. Optimize for
clarity over cleverness. Don't introduce frameworks or abstractions that
obscure the DDD lesson.

## Current state

- **Scala 3.3.4 LTS / Java 21 / sbt 1.10.** `sbt compile`, `sbt Test/compile`,
  and `sbt test` are all green on `rewrite/from-java-master` (**73 tests**).
- Test stack: ScalaTest 3.2.20 + `scalatestplus-mockito-5-12` + ScalaCheck 1.19.
- Spring Boot 3.3.10 / Jakarta EE 10 / Hibernate 6 (via starter-data-jpa) /
  ActiveMQ Classic 6.1.5.
- All 17 phases of [`.claude/plans/rewrite-from-java.md`](.claude/plans/rewrite-from-java.md)
  are complete; commit history is one tagged commit per phase.

**Locked-in design decisions (D1–D5):**

| ID | Decision |
| -- | -------- |
| D1 | Separate persistence model — domain stays JPA-annotation-free. JPA adapters land in 9b (follow-up). |
| D2 | Opaque types for ids (`TrackingId`, `UnLocode`, `VoyageNumber`). |
| D3 | Plain `require` for argument validation; `Objects.requireNonNull` for opaque-type null checks (Scala 3 forbids `== null` on opaque types). |
| D4 | ScalaTest 3.2 `AnyFunSuite with Matchers` + Mockito + ScalaCheck. |
| D5 | Spring Web MVC + Jackson (REST + JSON), no JSP. |

## Follow-up work (don't surprise the next session)

- **Phase 9b — JPA adapters.** With D1's separate persistence model, each of
  the four aggregates needs: JPA entity + mapper + Spring Data interface +
  adapter implementing the domain trait. In-memory repos (phase 9a) suffice
  for tests right now. `application.properties` excludes JPA/JMS autoconfig;
  remove those excludes when the adapters land.
- **`SampleDataGenerator`.** The `@PostConstruct` `loadHibernateData`
  orchestration that bootstraps `ABC123` / `JKL567` cargos with handling
  histories is not yet ported. Sample voyages and locations are wired up;
  just the orchestration is missing.
- **`messages*.properties` bundle.** `CargoTrackingDTOConverter` resolves
  locale-aware status text via Spring `MessageSource`. Keys like
  `cargo.status.IN_PORT`, `deliveryHistory.eventDescription.LOAD` need a
  resource bundle.
- **`UploadDirectoryScanner`.** Upstream's filesystem-watch handling-report
  ingester is not ported — only the REST `POST /handlingReport` path is.
- **Strict scalac flags.** `-Wunused:imports` is on (zero warnings).
  `-Wvalue-discard` and `-Xfatal-warnings` are still off — turning them on
  requires sprinkling `: Unit` ascriptions over Spring's void setters / JMS
  sends.

## Layout

```
src/main/scala/se/citerus/dddsample/
  domain/
    model/{cargo,handling,location,voyage}/   ← aggregates (framework-free)
    service/RoutingService                    ← domain service trait
    shared/                                   ← Entity, ValueObject, Specification
  application/                                ← orchestration traits (no domain logic)
  application/impl/                           ← @Transactional Spring impls
  application/util/DateUtils                  ← test-friendly Instant parsers
  infrastructure/
    persistence/inmemory/                     ← in-memory repos (phase 9a)
    routing/ExternalRoutingService            ← adapter to pathfinder API
    sampledata/{SampleLocations,SampleVoyages}
    messaging/jms/                            ← ActiveMQ-backed ApplicationEvents + consumers
  interfaces/
    booking/facade/                           ← BookingServiceFacade + DTOs + assemblers
    booking/web/CargoAdminController          ← REST /admin endpoints
    tracking/ws/                              ← REST /api/track/{id}
    handling/{ws,HandlingReportParser,...}    ← REST /handlingReport

src/main/scala/com/pathfinder/
  api/                                        ← routing-team's interface
  internal/                                   ← in-process impl

src/test/scala/se/citerus/dddsample/
  scenario/CargoLifecycleScenarioTest         ← end-to-end book→route→handle scenario
```

## Conventions

- **DDD discipline.** `domain.*` never imports from `infrastructure.*` or
  `interfaces.*`. Application services orchestrate; aggregates own
  invariants. See `.claude/skills/scala-ddd-tactical/SKILL.md`.
- **Opaque types** are zero-cost wrappers — they're `String` at runtime.
  Don't compare with `null`; use `Objects.requireNonNull` instead.
- **Domain stays framework-free.** No JPA, no Spring, no Jackson
  annotations on `domain.*` types. JPA adapters translate.
- **Tests mirror source packages**, one suite per production class.
- **Spring-coupled tests** mock the repositories with Mockito; the scenario
  test wires the *real* application services to the *in-memory* infra.
- **Skills:**
  - `.claude/skills/scala-ddd-tactical/SKILL.md` — invoke when adding to or
    refactoring the domain model.
  - `.claude/skills/scala-testing/SKILL.md` — invoke when writing or porting
    tests.
  - `.claude/skills/scala3-migration/SKILL.md` — Scala 3 idioms; useful when
    a Java upstream pattern doesn't translate cleanly.

## Common commands

```bash
sbt compile                       # main compile
sbt test                          # all 73 tests
sbt "testOnly *CargoTest"         # one suite
sbt scalafmtAll                   # format
sbt scalafmtCheckAll              # CI-style format check
sbt run                           # boot Spring Boot on :8080
```

Java 21 + sbt 1.10 are required. `brew install sbt openjdk@21`.

## Dependency hygiene

- `.github/dependabot.yml` — weekly sbt + GitHub Actions bumps, grouped by
  family (Spring, Hibernate, ActiveMQ, Jackson, logging, test frameworks).
  Defers `org.scala-lang*` to Scala Steward.
- `.scala-steward.conf` — repo-specific Scala Steward config. Enroll the
  repo in the relevant `scala-steward-repos` list.

## What NOT to do

- Don't add Cats Effect / ZIO / Akka to the existing bounded contexts.
  The sample is deliberately framework-light. New experimental contexts
  may use them, but isolate them.
- Don't put JPA annotations on `domain.*` classes — that breaks D1. Add a
  separate JPA entity + mapper instead.
- Don't `--no-verify` past pre-commit hooks. If a hook fails, fix the cause.
- Don't re-enable `-Xfatal-warnings` in `build.sbt` without first adding
  the explicit `: Unit` ascriptions `-Wvalue-discard` will demand.
