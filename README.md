# ddd-sample-scala

[![CI](https://github.com/oluies/ddd-sample-scala/actions/workflows/ci.yml/badge.svg)](https://github.com/oluies/ddd-sample-scala/actions/workflows/ci.yml)
[![Scala](https://img.shields.io/badge/scala-3.3.4_LTS-DC322F?logo=scala&logoColor=white)](https://www.scala-lang.org/)
[![Java](https://img.shields.io/badge/java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.3.10-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Mill](https://img.shields.io/badge/mill-1.1.6-5697C4?logo=scala&logoColor=white)](https://mill-build.org/)
[![Tests](https://img.shields.io/badge/tests-73%20passing-brightgreen)](#status)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue?logo=scala&logoColor=white)](https://github.com/scala-steward-org/scala-steward)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](license.txt)

A Scala port of the [Citerus DDD Sample](https://github.com/citerus/dddsample-core)
— Eric Evans' canonical cargo-shipping example used to teach Domain-Driven
Design tactical patterns: entities, value objects, aggregates, repositories,
domain services, domain events, and specifications.

This is a **teaching artifact**, not a production system. Optimize for
clarity over cleverness.

## Status

The `rewrite/from-java-master` branch is a clean rewrite from the Java
upstream's current `Spring Boot 3.3 / Jakarta EE / Hibernate 6` baseline,
targeting **Scala 3.3.4 LTS** on **Java 21** and **Mill 1.1.6**. All 17
phases of the rewrite plan are complete; `mill ddd.test` is green
(73 tests).

| Component        | Version              | Notes                                     |
| ---------------- | -------------------- | ----------------------------------------- |
| Scala            | 3.3.4 LTS            | dialect: `-source:3.3`                    |
| Java             | 21                   | `--release 21` baseline                   |
| Build            | Mill 1.1.6           | pinned in `.mill-version`                 |
| Spring Boot      | 3.3.10               | starter-web, starter-data-jpa, activemq, actuator, validation, thymeleaf |
| Jakarta EE       | 10                   | via Spring Boot 3.3 BOM                   |
| Hibernate        | 6.x                  | transitive via `spring-boot-starter-data-jpa` |
| ActiveMQ Classic | 6.1.5                | Jakarta variant                           |
| Test framework   | ScalaTest 3.2.20     | + `scalatestplus-mockito-5-12`            |
| Property tests   | ScalaCheck 1.19.0    |                                           |

## Design decisions (rewrite branch)

| ID  | Decision                                              | Rationale                                          |
| --- | ----------------------------------------------------- | -------------------------------------------------- |
| D1  | Separate persistence model — domain stays JPA-annotation-free | Keeps the DDD lesson visible; JPA adapters land in `infrastructure.persistence.jpa` (follow-up phase 9b) |
| D2  | Opaque types for ids (`TrackingId`, `UnLocode`, `VoyageNumber`) | Zero-cost wrappers that disappear at runtime but stay distinct at compile time |
| D3  | Plain `require` for argument validation; `Objects.requireNonNull` for opaque-type null checks | `require` rejects empty/invalid; opaque types need the Java helper because Scala 3 forbids comparing them with `null` |
| D4  | ScalaTest 3.2 `AnyFunSuite with Matchers` + Mockito + ScalaCheck | Same idioms used across `domain.*` and `application.*` test suites |
| D5  | Spring Web MVC + Jackson (REST + JSON), no JSP        | `CargoAdminController` and `HandlingReportServiceImpl` are clean `@RestController`s |

See [`.claude/plans/rewrite-from-java.md`](.claude/plans/rewrite-from-java.md)
for the full 17-phase plan; commit history on this branch is one tagged
commit per phase.

## Layout

```
src/main/scala/se/citerus/dddsample/
  domain/
    model/{cargo,handling,location,voyage}/   ← aggregates (framework-free)
    service/                                  ← RoutingService trait
    shared/                                   ← Entity, ValueObject, Specification
  application/                                ← BookingService, HandlingEventService,
                                                CargoInspectionService, ApplicationEvents
  application/impl/                           ← @Transactional Spring impls
  application/util/DateUtils                  ← test-friendly Instant parsers
  infrastructure/
    persistence/inmemory/                     ← in-memory repository impls (phase 9a)
    routing/ExternalRoutingService             ← adapter calling the pathfinder API
    sampledata/                                ← SampleLocations, SampleVoyages
    messaging/jms/                             ← ActiveMQ ApplicationEvents impl + consumers
  interfaces/
    booking/facade/                            ← BookingServiceFacade + DTOs + assemblers
    booking/web/CargoAdminController           ← REST /admin endpoints
    tracking/ws/                               ← REST /api/track/{id} + DTO converter
    handling/{ws,HandlingReportParser,...}     ← REST /handlingReport

src/main/scala/com/pathfinder/
  api/{GraphTraversalService, TransitEdge, TransitPath}    ← routing-team's API
  internal/{GraphDAO, GraphDAOStub, GraphTraversalServiceImpl}  ← in-process impl

src/test/scala/se/citerus/dddsample/
  domain/, application/, infrastructure/      ← unit + property tests
  scenario/CargoLifecycleScenarioTest          ← end-to-end book-route-handle scenario
```

The DDD invariant: `domain.*` never imports from `infrastructure.*` or
`interfaces.*`.

## Build & run

```bash
mill ddd.compile                  # compile main sources
mill ddd.test                     # run all tests (73 currently)
mill 'ddd.test.testOnly *CargoTest'   # run a single suite
mill __.reformat                  # format the codebase
mill __.checkFormat               # CI-style format check
mill ddd.run                      # boot Spring Boot on :8080
```

Requires JDK 21 and Mill on the PATH. Install Mill with `brew install mill`
or via [Coursier](https://get-coursier.io/) (`cs install mill`). The
`.mill-version` file pins the Mill version; both installers honour it.

## Dependency management

- **Scala Steward** ([`.scala-steward.conf`](.scala-steward.conf)) — handles
  every JVM dependency in `build.mill` plus the Mill version itself. Enroll
  by adding `oluies/ddd-sample-scala` to your `scala-steward-repos` list;
  Steward picks up the repo-local config automatically.
- **Dependabot** ([`.github/dependabot.yml`](.github/dependabot.yml)) —
  GitHub Actions workflows only. Dependabot has no `mill` package
  ecosystem, so JVM-side bumps live with Scala Steward.

## Working with Claude Code

Project-local skills live under `.claude/skills/`:

- **`scala-ddd-tactical`** — invoke when adding to or refactoring the
  domain model.
- **`scala-testing`** — invoke when writing or porting tests.
- **`scala3-migration`** — leftover from the patch-forward migration on
  master; still useful for general Scala 3 idioms.

`CLAUDE.md` at the repo root has the short briefing for the assistant.

## What's NOT done (follow-up work)

- **Phase 9b — JPA persistence adapters.** The "separate persistence model"
  decision (D1) requires a JPA-annotated mirror class + mapper + Spring Data
  interface + adapter for each of the four aggregates. The in-memory repos
  from phase 9a are sufficient for tests and a lightweight demo, but JPA
  has not been wired up. The Spring Boot autoconfig excludes for JPA / JMS
  in `application.properties` need to be removed once the adapters land.
- **`SampleDataGenerator`.** Upstream's bootstrap loader (creating the
  `ABC123` and `JKL567` reference cargos with handling histories) is not
  yet ported. The named voyages and locations are wired up in
  `SampleLocations` / `SampleVoyages`; what's missing is the
  `@PostConstruct` `loadHibernateData` orchestration.
- **Messages bundle.** `CargoTrackingDTOConverter` resolves locale-aware
  status/description text via Spring `MessageSource`; the
  `messages*.properties` resource bundles need to land before the tracking
  endpoint returns useful strings.
- **`HandlingReportParser` / file scanner.** Upstream's filesystem-watch
  variant (`UploadDirectoryScanner` reading CSV from a watched directory)
  is not ported — only the REST `POST /handlingReport` path is.

## Reference

- Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of
  Software*, Addison-Wesley, 2003.
- [Citerus DDD Sample (Java)](https://github.com/citerus/dddsample-core).
- [Scala 3 migration guide](https://docs.scala-lang.org/scala3/guides/migration/compatibility-intro.html).

## License

See [`license.txt`](license.txt) — original Citerus license preserved.

## See also

- `master` branch — the Scala 2.8 → 3.3 patch-forward migration of the
  pre-Spring-Boot Java codebase. Different starting point, same destination
  language version.
- [`.claude/plans/rewrite-from-java.md`](.claude/plans/rewrite-from-java.md) —
  the 17-phase rewrite plan and design decisions.
