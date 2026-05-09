# Plan: rewrite from the modern Java reference

**Status:** Draft, 2026-05-09
**Companion to:** `.claude/todo/upstream-comparison.md`
**Supersedes (eventually):** the patch-forward roadmap in
`.claude/todo/follow-ups.md`. That roadmap stays as historical record.

## Premise

The just-merged Scala 3 migration (PR #77) brought the codebase to a
shippable Scala 3.3 / Java 17 / sbt baseline. But the *upstream Java
reference* (`citerus/dddsample-core`) has moved to Spring Boot 3.3 /
Jakarta EE / Hibernate 6 with REST + JMS + JPA wired up, and has 51
tests vs. our 5. Catching up by patching is essentially reimplementing
the modern Java stack one piece at a time on top of a 15-year-old
skeleton — by the time we'd finish, almost nothing of the original
source survives. A clean rewrite is the same total effort, with a
better outcome and a cleaner `git blame`.

## Goals

1. **Functional parity with the upstream Java reference**, in Scala 3
   idiomatic form. Same domain model, same use cases, same interfaces
   (REST tracking, REST booking-admin, SOAP-style file/ws handling
   reports, JMS messaging).
2. **Honor the DDD invariant** the existing port already declares:
   `domain.*` never imports from `infrastructure.*` or `interfaces.*`.
   The rewrite finally lives up to it by separating the persistence
   model from the domain model (no more `@BeanProperty` on domain
   classes).
3. **Scala-3 native idioms throughout.** Opaque types for ids, `enum`
   for closed states, `given` / `using` for cross-cutting context,
   extension methods for adapters, no `return`, no procedure syntax.
4. **Inherit upstream test design.** Each Java test maps to a Scala
   suite; behavior coverage matches.
5. **Reuse the migration infrastructure** that PR #77 produced —
   `build.sbt`, `.github/` workflows, scalafmt config, Dependabot,
   Scala Steward, the `.claude/` skills and plans.

## Non-goals

- **Bytecode / artifact compatibility** with the Java reference.
- **1:1 file mapping.** Where Scala 3 lets us collapse two Java classes
  into one (e.g., a sealed-trait file + companion), do it.
- **Effect systems.** No `cats-effect` / `ZIO`. Java reference is
  synchronous Spring; the rewrite stays synchronous. Reactive
  experiments are a different project.
- **Pure-functional purity.** Domain methods that mutate aggregate
  state in place are fine — that's the Java reference's pattern and
  it's the DDD-textbook idiom. Don't fight it.

## Branch strategy

- Long-running branch: `rewrite/from-java-master`, off current
  `master`.
- Sub-branches per phase, PR'd into the long-running branch:
  `rewrite/01-foundation`, `rewrite/02-domain-core`, etc.
- Rebase against `master` periodically to absorb Dependabot / Scala
  Steward bumps. (Don't merge `master` in — keep the rewrite branch's
  history linear so review is easy.)
- Final landing: one squash-merge of `rewrite/from-java-master` into
  `master` once everything is green. Or: a series of merges, one per
  phase, if the team prefers reviewable slices.

## Decisions to make first

These are the cross-cutting decisions that shape everything downstream.
Make them before phase 2.

### D1 — Persistence model: separate or annotated domain?

**Option A (recommended): separate persistence model.**
Domain stays framework-free. JPA `@Entity` classes live in
`infrastructure/persistence/jpa/` and are *not* the domain types —
they're DTOs that get mapped to/from domain types in the repository.

Pros: respects the DDD invariant, makes domain testable without
Hibernate, allows opaque-type ids on domain side.

Cons: more boilerplate (one mapper per aggregate). Java reference goes
the other way (annotates domain), so this is a deliberate divergence.

**Option B: annotate domain like Java does.**
`@Entity`, `@Id`, `@ManyToOne` on `Cargo`, `Voyage`, etc.

Pros: matches Java reference; less code.
Cons: violates the DDD invariant the Scala port already preaches in
`scala-ddd-tactical/SKILL.md`.

### D2 — Id types: opaque or case class?

**Option A (recommended): opaque types** for `TrackingId`, `UnLocode`,
`VoyageNumber`, `CarrierMovementId`. Zero allocation, no
`equals`/`hashCode` boilerplate. Forces an explicit smart constructor
that runs validation.

Cons: opaque types don't survive Java-reflection-based serialization
(Jackson, JPA) cleanly. Need explicit converters at the boundary —
which is fine if we're already going with D1's separate persistence
model.

### D3 — Validation library

**Option A (recommended): plain `require`.** Matches Java
`Validate.notNull` style. No new dep.

**Option B:** `commons-lang3 Validate` (we have it). Same as Java
reference.

**Option C:** `refined` / `iron`. Compile-time validation for ids.
Powerful, but a teaching-artifact project shouldn't reach for
type-level tricks unless they earn it.

### D4 — Test stack

**ScalaTest 3.2 `AnyFunSuite` + `Matchers` + `MockitoSugar` +
ScalaCheck.** Already on the classpath. Matches what we already wrote
in PR #77.

### D5 — HTTP

**Spring Web MVC + Jackson.** Matches Java reference. Don't introduce
http4s / tapir.

## Phased rollout

Each phase is one or more PRs, each green on its own. Don't move to
phase N+1 until phase N is in.

### Phase 0 — Branch + build flip (1 day)

- [ ] Cut `rewrite/from-java-master` off current `master`.
- [ ] Update `build.sbt`: bump to Spring Boot 3.3.10 starter parent
      equivalents (`spring-boot-starter-web`,
      `spring-boot-starter-data-jpa`, `spring-boot-starter-jms`,
      `spring-boot-starter-test`), Hibernate 6 (transitively),
      Jackson, embedded Tomcat. Drop CXF (replaced by REST). Drop
      explicit Spring 5 modules.
- [ ] Bump Java baseline to 21 (Spring Boot 3.3 supports it).
- [ ] Replace `xsbt-web-plugin` with `sbt-native-packager` (or the
      Spring Boot sbt plugin) for `runMain` / fat-jar packaging.
- [ ] Delete `src/main/webapp/`. Embedded Tomcat replaces it.
- [ ] Delete `src/main/scala/` wholesale. Build will fail; that's
      expected.
- [ ] `sbt compile` — should succeed on an empty source tree.

**Exit criteria:** sbt loads on Spring Boot 3 deps; empty source tree
compiles; CI workflow still runs.

### Phase 1 — Foundation: `Application` + `config` (1–2 days)

Translate the Java reference's bootstrap layer.

- [ ] `se.citerus.dddsample.Application` (`@SpringBootApplication`).
- [ ] `se.citerus.dddsample.config.DDDSampleApplicationContext` —
      Java `@Configuration` for cross-cutting beans.
- [ ] `se.citerus.dddsample.interfaces.InterfacesApplicationContext`.
- [ ] `com.pathfinder.config.PathfinderApplicationContext`.
- [ ] Smoke test: `sbt run` boots the app on `:8080`, `/actuator/health`
      returns 200.

**Exit criteria:** app boots with no domain code; `/actuator/health` OK.

### Phase 2 — Domain shared (`domain.shared`) (half day)

- [ ] `Entity[T]` trait — identity-based equality.
- [ ] `ValueObject[T]` trait — structural equality.
- [ ] `DomainEvent[T]` trait.
- [ ] `Specification[T]` + `AbstractSpecification[T]` +
      `And` / `Or` / `Not` composers.
- [ ] **Tests:** port `domain/shared/AbstractSpecificationTest.java`
      and friends → `SpecificationTest`.

**Exit criteria:** `domain/shared/` compiles + tests pass.

### Phase 3 — Domain `model.location` (half day)

- [ ] `UnLocode` — opaque type (per D2) with smart constructor running
      regex validation.
- [ ] `Location` — entity wrapping `UnLocode` and a name. Identity
      equals on UnLocode.
- [ ] `LocationRepository` trait.
- [ ] **Tests:** port `LocationTest`, `UnLocodeTest` from Java.
      Add ScalaCheck property test for UnLocode validation.

### Phase 4 — Domain `model.voyage` (half day)

- [ ] `VoyageNumber` — opaque type.
- [ ] `CarrierMovement` — value object.
- [ ] `Schedule` — list of carrier movements + invariants.
- [ ] `Voyage` — entity, identity on `VoyageNumber`.
- [ ] `VoyageBuilder` — companion-object factory.
- [ ] `VoyageRepository` trait.
- [ ] **Tests:** port `VoyageTest`, `VoyageBuilderTest` if Java has
      them; otherwise lift property tests from `Schedule` invariants.

### Phase 5 — Domain `model.handling` (1 day)

- [ ] `HandlingEventType` — Scala 3 `enum` with `voyageRequired`
      property.
- [ ] `HandlingEvent` — `DomainEvent`.
- [ ] `HandlingHistory` — value object aggregating events.
- [ ] `HandlingActivity` — value object describing predicted activity.
- [ ] `HandlingEventFactory` — application-tier factory; throws
      `Unknown{Cargo,Voyage,Location}Exception`.
- [ ] `CannotCreateHandlingEventException`.
- [ ] `HandlingEventRepository` trait.
- [ ] **Tests:** port `HandlingEventTest`,
      `HandlingEventFactoryTest`, `HandlingEventHistoryTest` from Java.

### Phase 6 — Domain `model.cargo` (1–2 days, biggest)

- [ ] `TrackingId` — opaque type.
- [ ] `RouteSpecification` — extends `AbstractSpecification[Itinerary]`.
- [ ] `Leg` — value object linking voyage + load/unload locations.
- [ ] `Itinerary` — list of legs, `isExpected(event)`, etc.
- [ ] `RoutingStatus`, `TransportStatus` — Scala 3 `enum`.
- [ ] `Delivery` — value object holding all derived state. **The big
      class.** Re-derives the no-`return` rewrite the migration
      started.
- [ ] `Cargo` — aggregate root, `assignToRoute`,
      `deriveDeliveryProgress`, `specifyNewRoute`.
- [ ] `CargoRepository` trait.
- [ ] **Tests:** port `CargoTest`, `RouteSpecificationTest`,
      `ItineraryTest`, `DeliveryTest`, `LegTest`, `TrackingIdTest` from
      Java. **This is where most of the test gap closes.**

### Phase 7 — Domain `service` (half day)

- [ ] `RoutingService` trait — same shape as Java.

### Phase 8 — Application services (1 day)

- [ ] `BookingService` + `Impl`.
- [ ] `CargoInspectionService` + `Impl`.
- [ ] `HandlingEventService` + `Impl`.
- [ ] `ApplicationEvents` trait (publish-side).
- [ ] **Tests:** port `BookingServiceTest`,
      `CargoInspectionServiceTest`, `HandlingEventServiceTest` from
      Java. Mockito-based.

### Phase 9 — Infrastructure: persistence (1–2 days)

- [ ] **JPA implementations** under `infrastructure.persistence.jpa`:
      `CargoRepositoryJPA`, `HandlingEventRepositoryJPA`,
      `LocationRepositoryJPA`, `VoyageRepositoryJPA`.
- [ ] **Persistence model** (per D1): `Cargo$Entity`,
      `HandlingEvent$Entity`, etc. Annotated with `@Entity`, `@Id`,
      `@ManyToOne`. Mappers (`CargoEntityMapper.toDomain` /
      `fromDomain`) bridge to domain types.
- [ ] **In-memory implementations** for tests under
      `infrastructure.persistence.inmemory` (matches Java reference).
- [ ] **Tests:** port the 5 JPA repository tests from Java reference,
      plus the 4 in-memory tests. These are integration tests against
      H2 / HSQLDB via Spring Boot's `@DataJpaTest`.

### Phase 10 — Infrastructure: routing + sample data (half day)

- [ ] `infrastructure.routing.ExternalRoutingService` — Spring bean
      that adapts the pathfinder graph service to a `RoutingService`.
- [ ] `infrastructure.sampledata.{SampleDataGenerator,
      SampleLocations, SampleVoyages}` — moved from
      `application.util/`.
- [ ] **Tests:** port routing test from Java if it has one.

### Phase 11 — Pathfinder (half day)

- [ ] `com.pathfinder.api.{GraphTraversalService, TransitEdge,
      TransitPath}`.
- [ ] `com.pathfinder.internal.{GraphDAO, GraphTraversalServiceImpl}`.
- [ ] `com.pathfinder.config.PathfinderApplicationContext`.

### Phase 12 — Interfaces: booking REST + admin web (1–2 days)

- [ ] `interfaces.booking.facade.BookingServiceFacade` + Impl.
- [ ] DTO classes (`CargoRoutingDTO`, `LegDTO`, `LocationDTO`,
      `RouteCandidateDTO`).
- [ ] DTO assemblers.
- [ ] `interfaces.booking.web.CargoAdminController` —
      `@Controller` + `@GetMapping` / `@PostMapping`. Replaces the
      stubbed `CargoTrackingController` from PR #77.
- [ ] `RegistrationCommand`, `RouteAssignmentCommand`.
- [ ] **Tests:** port assembler tests + `CargoAdminControllerTest`
      from Java reference.

### Phase 13 — Interfaces: tracking REST (1 day)

- [ ] `interfaces.tracking.ws.CargoTrackingRestService` —
      `@RestController` returning `CargoTrackingDTO`.
- [ ] `CargoTrackingDTO`, `HandlingEventDTO`,
      `CargoTrackingDTOConverter`.
- [ ] **Tests:** port tracking REST tests from Java reference.

### Phase 14 — Interfaces: handling (1 day)

- [ ] `interfaces.handling.HandlingReportParser`.
- [ ] `interfaces.handling.HandlingEventRegistrationAttempt`.
- [ ] `interfaces.handling.file.UploadDirectoryScanner` —
      directory-watch implementation.
- [ ] `interfaces.handling.ws.{HandlingReport, HandlingReportService,
      HandlingReportServiceImpl}` — REST handling-report endpoint.
      Subsumes the existing `com.aggregator.HandlingReport` (delete
      the `com.aggregator` package).

### Phase 15 — Infrastructure: messaging (JMS) (1 day)

- [ ] `infrastructure.messaging.jms.{CargoHandledConsumer,
      HandlingEventRegistrationAttemptConsumer,
      InfrastructureMessagingJmsConfig, JmsApplicationEventsImpl,
      SimpleLoggingConsumer}`.
- [ ] **Tests:** port the in-memory `ApplicationEvents` stub from
      Java's `infrastructure/messaging/stub/`.

### Phase 16 — Acceptance / scenario tests (1–2 days, optional)

- [ ] Port end-to-end scenario test from Java's
      `src/test/java/se/citerus/dddsample/scenario/`.
- [ ] **Skip** the `acceptance/pages/*` Selenium-style tests unless
      someone wants UI test coverage. They were removed from this
      project's scope by Citerus and add a heavy dependency.

### Phase 17 — Polish (1 day)

- [ ] Re-enable strict scalac flags from
      `.claude/todo/follow-ups.md` item A.
- [ ] Run `sbt scalafix` for any cleanup rules
      (`OrganizeImports`, `RemoveUnused`).
- [ ] `sbt scalafmtAll`.
- [ ] Verify all skills in `.claude/skills/` still describe the
      codebase accurately. Update if not.
- [ ] Update `README.md` and `CLAUDE.md` to reflect the rewrite.
- [ ] Add a "what changed in the rewrite" doc analogous to the
      migration notes.

## Total effort estimate

Phase | Effort
----- | ------
0–1   | 2–3 days
2–7   | 4–5 days (domain core)
8–11  | 3–4 days (application + infrastructure non-interface)
12–15 | 4–5 days (interfaces + JMS)
16–17 | 2–3 days
**Total** | **~3 weeks of focused work**

For comparison: the patch-forward path would total ~2 weeks if everything
went smoothly, but each step interacts with existing legacy choices
(BeanProperty stays on domain, XML mappings stay, com/aggregator stays,
WAR packaging stays unless we explicitly tear it out) — so realistically
it's the same effort with a worse outcome.

## Risks

| Risk                                                              | Mitigation                                                       |
| ----------------------------------------------------------------- | ---------------------------------------------------------------- |
| Scope creep — "while we're at it" rewrites in domain semantics    | Java reference is the spec. If a method does X in Java, it does X in Scala. Refinements are follow-up PRs after the rewrite lands. |
| Opaque types break Hibernate / Jackson reflection                 | D1's separate-persistence-model + explicit Jackson serializers handle this. Don't try to make opaque types reflect themselves. |
| Lost git history on the source tree                               | Acceptable — old history lives in `master` up to commit `f027661`. The rewrite is a clean break. |
| Spring Boot 3 deps churn the lockfile                             | Let Dependabot/Steward handle bumps post-rewrite. Don't pin everything. |
| The rewrite branch goes stale relative to `master`                | Rebase against master weekly. If master gets significant changes (security fixes, etc.), forward-port them as cherry-picks. |
| Test ports diverge from Java semantics                            | Each test PR keeps the Java original alongside as a comment until reviewed. Translate behavior, not syntax. |

## What carries over from the current `master`

- `build.sbt` — stays, gets rewritten substantively but the structure is
  the same.
- `.github/dependabot.yml` — needs ecosystem update if we move to
  `package-ecosystem: "gradle"` (we won't — sbt remains).
- `.github/workflows/ci.yml` — stays. Update Java version to 21.
- `.scalafmt.conf` — stays.
- `.scala-steward.conf` — stays.
- `.aider.conf.yml` (gitignored) — stays.
- `.claude/skills/*.md` — stay; update slightly for new layout.
- `.claude/plans/scala3-upgrade.md` — historical record, doesn't move.
- `.claude/plans/rewrite-from-java.md` — this file.
- `.claude/todo/*.md` — stay; `tests-to-add.md` becomes mostly obsolete
  (tests get added during the rewrite); `follow-ups.md` items A, B,
  G are subsumed by the rewrite; items C, D become *part* of the
  rewrite; items E, F are now phase 0 and phase 9.
- `LICENSE` — stays.

## What gets thrown away

- All of `src/main/scala/` and `src/main/webapp/`.
- `src/main/resources/*.xml` (Spring XML, Hibernate `*.hbm.xml`).
  Replaced with `@Configuration` + JPA annotations.
- `src/test/scala/` (5 stubs + 3 ported tests). Rewrite phase ports the
  Java reference's 51 tests instead.
- The `com/aggregator/` package — folded into
  `interfaces/handling/ws/` per the Java layout.

## Decision checkpoints

Before starting:

1. **Confirm D1** — separate persistence model vs. annotated domain.
   Recommended: separate. If you'd rather match Java exactly, switch
   to annotated and skip the `*EntityMapper` boilerplate.
2. **Confirm D2** — opaque types vs case-class wrappers. Recommended:
   opaque, given D1.
3. **Confirm Java baseline** — 17 (current) or 21 (Spring Boot 3.3
   supports it; modern Scala 3 prefers it).

Before merging the long-running branch:

4. **Smoke test** the booted app: `sbt run`, hit the REST endpoints,
   verify a sample cargo journey end-to-end (the upstream
   `scenario/` test should be the spec).
5. **Performance sanity check** — Spring Boot 3 + Hibernate 6 vs
   Spring 5 + Hibernate 5 has different startup time and memory
   characteristics. Document any surprises.

## Out of scope (deliberately)

- **Functional architecture** (ports & adapters with cats-effect).
  Pedagogical goal is "what DDD looks like in Scala 3 with Spring",
  not "what hexagonal architecture looks like with effect systems".
- **CQRS / event sourcing.** The Java reference doesn't do them; we
  don't either.
- **Build tool change** to Mill / Bazel / Gradle. sbt stays.
- **Multi-module project structure.** Single sbt module mirrors
  upstream.
