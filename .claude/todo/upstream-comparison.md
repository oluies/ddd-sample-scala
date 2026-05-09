# Comparison with the Java reference (citerus/dddsample-core)

**Created:** 2026-05-09
**Reference:** <https://github.com/citerus/dddsample-core> (default branch, snapshot
2026-05-09).
**Audience:** Anyone deciding what to port forward into the Scala version.

The Java reference is the canonical implementation of Eric Evans' cargo
sample. The Scala port lagged behind for ~15 years; it was built against a
2010-era Spring 2/3 stack and a Maven 2 build, and has just (PR #77) been
brought to Scala 3 / sbt / Java 17. The Java reference, in contrast, is on
**Spring Boot 3.3.10 / Jakarta EE / Hibernate 6** with REST + JMS + JPA
all wired up.

This file inventories the gap so the next round of work can be scoped.

## Headline numbers

|                          | Java reference | Scala port (this repo) |
| ------------------------ | -------------- | ---------------------- |
| Main source files        | 94             | 74                     |
| Test source files        | 51             | 5                      |
| Build                    | Maven (Spring Boot starter parent 3.3.10) | sbt 1.10 |
| Web framework            | Spring Boot 3.3.10                | Spring 5.2.19 + WAR |
| Servlet API              | Jakarta EE (`jakarta.*`)          | Java EE (`javax.*`) |
| ORM                      | Hibernate 6 (via Boot starter) + JPA | Hibernate 5.4 (no JPA) |
| HTTP API                 | REST (Spring MVC + Jackson)       | SOAP (CXF JAX-WS) |
| Bootstrap                | `Application.java` `@SpringBootApplication` | XML + `web.xml` + WAR |
| Spring config            | Java `@Configuration` classes     | XML files in `webapp/WEB-INF/` |
| Build / run              | `./mvnw spring-boot:run`          | `sbt "Jetty/start"` |

## Stack-level differences

The Java reference is **on a fundamentally different platform** than the
Scala port. Closing the gap involves three big jumps:

1. **Spring 5 → Spring Boot 3.x.** This is Phase E in
   `follow-ups.md` — flips every `javax.*` to `jakarta.*`, replaces
   `web.xml` + WAR + Jetty with `@SpringBootApplication` + embedded
   Tomcat, replaces XML wiring with `@Configuration` classes.
2. **Hibernate 5 → Hibernate 6 + JPA.** Java reference exposes both a
   Hibernate-native and a JPA implementation of each repository
   (`*RepositoryHibernate` and `*RepositoryJPA`). The Scala port only
   has the Hibernate side, and the integration tests are stubbed.
3. **CXF SOAP → Spring MVC REST.** Java reference uses Jackson + REST;
   the Scala port still uses CXF JAX-WS. Migration would be a
   straightforward port of the booking-facade and tracking endpoints.

## Packages the Java reference has, and we don't

| Java package                                       | Purpose                                                                 | Effort |
| -------------------------------------------------- | ----------------------------------------------------------------------- | ------ |
| `se.citerus.dddsample.Application`                 | Spring Boot main class.                                                 | trivial; tied to Phase E |
| `se.citerus.dddsample.config.*`                    | `@Configuration` classes replacing the Spring XML.                      | medium; tied to Phase E |
| `com.pathfinder.config.PathfinderApplicationContext` | Pathfinder service Spring config.                                     | trivial; tied to Phase E |
| `se.citerus.dddsample.infrastructure.persistence.jpa.*` | JPA implementations of the 4 repositories (Cargo / HandlingEvent / Location / Voyage). | medium |
| `se.citerus.dddsample.infrastructure.sampledata.*` | Sample data generator (moved here from `application/util/`).            | small (move + minor refactor) |
| `se.citerus.dddsample.infrastructure.messaging.jms.*` | JMS infrastructure: `CargoHandledConsumer`, `HandlingEventRegistrationAttemptConsumer`, `JmsApplicationEventsImpl`, etc. | medium |
| `se.citerus.dddsample.interfaces.booking.web.*`    | `CargoAdminController`, `RegistrationCommand`, `RouteAssignmentCommand` — the modern booking-admin web UI. | medium |
| `se.citerus.dddsample.interfaces.handling.file.UploadDirectoryScanner` | Watches a directory for handling-event reports uploaded as files.       | small |
| `se.citerus.dddsample.interfaces.handling.ws.*`    | SOAP-style handling report service (`HandlingReportService` + impl).    | small (Scala already has the CXF aggregator equivalent in `com/aggregator/HandlingReport.scala` — could converge) |
| `se.citerus.dddsample.interfaces.tracking.ws.*`    | **REST tracking API** — `CargoTrackingRestService`, DTOs, converter.    | medium; high user-facing value |

**Already-in-Scala equivalents that may be obsolete:**
- `com.aggregator.HandlingReport` — duplicates `interfaces/handling/ws/HandlingReport`. The Java reference moved it under `interfaces.handling.ws.*`. Could fold into the Scala `interfaces/` tree to match.

## Packages we have, that Java *removed*

The Scala port is structurally close — there's only one stale package:

- `com/aggregator/` (Scala-only) — should be folded under `interfaces/handling/ws/` to match the Java layout.

## Test coverage gap

The Java reference has **51 tests across 19 test packages**; the Scala port
has **5 tests across 4 packages**. Java test packages, with file counts:

```
6  acceptance/pages/                  (Selenium-style page objects)
5  domain/model/cargo/                (Cargo + RouteSpec + Itinerary + Delivery + Leg)
5  domain/shared/                     (Specification, Entity, ValueObject + composers)
5  infrastructure/persistence/jpa/    (per-repository JPA tests)
4  infrastructure/persistence/inmemory/ (in-memory test doubles for repositories)
3  acceptance/                        (acceptance scenarios)
3  domain/model/handling/             (HandlingEvent + Factory + History)
3  interfaces/booking/facade/internal/assembler/ (DTO assembler tests)
3  interfaces/tracking/               (the tracking controller path)
2  application/                       (BookingService + HandlingEventService)
2  domain/model/location/             (Location + UnLocode)
2  interfaces/handling/               (handling parser + integration)
2  interfaces/tracking/ws/            (REST tracking)
1  scenario/                          (end-to-end cargo journey)
1  domain/model/voyage/               (Voyage)
1  infrastructure/messaging/stub/     (in-memory ApplicationEvents stub)
1  infrastructure/routing/            (ExternalRoutingService)
1  interfaces/booking/web/            (CargoAdminController)
1  interfaces/handling/file/          (UploadDirectoryScanner)
```

Most of the gaps in `.claude/todo/tests-to-add.md` line up directly with
Java tests we could port:

- Item 1 (`Delivery`) → Java has `domain/model/cargo/DeliveryTest.java`.
- Item 2 (value objects) → Java has `LocationTest`, `UnLocodeTest`,
  `VoyageNumberTest`, `TrackingIdTest`.
- Item 3 (Specification composers) → Java has
  `domain/shared/AbstractSpecificationTest.java` and friends.
- Item 4 (`Itinerary`) → Java has `ItineraryTest.java`.
- Item 5 (`HandlingHistory`) → Java has `HandlingEventHistoryTest.java`.
- Item 6 (factory exceptions) → Java has `HandlingEventFactoryTest.java`.

**Recommendation:** when adding tests on the Scala side, pull cases
straight from the Java reference and translate them to ScalaTest. The
business-rule coverage is already designed; we just need to translate.

## DDD-aspect differences

The Java reference reflects ~15 years of refinement that the Scala port
hasn't absorbed. A few notable ones:

- **Sample data is in `infrastructure.sampledata`**, not
  `application.util`. The Scala port should follow — sample data is
  infrastructure concern (DB seeding), not application orchestration.
- **JPA implementations alongside Hibernate-native.** Demonstrates that
  the repository abstraction is real — the same domain works against
  both ORM strategies. Worth adding.
- **REST tracking API** is a separate package. The Scala port has only
  the SOAP booking facade — adding REST would let the project demonstrate
  multiple interface styles cleanly.
- **Scenario / acceptance tests** in their own packages
  (`scenario/`, `acceptance/`). The Scala port has neither tier.

## Suggested rollout if catching up to Java

1. **Sample data move** — small, no behavior change. Move
   `application.util.SampleDataGenerator` → `infrastructure.sampledata`.
   Mirror Java layout.
2. **Test ports** — items 1-6 in `tests-to-add.md`. Each Scala test can
   start from the Java equivalent.
3. **Spring Boot 3 / Jakarta migration** (Phase E in
   `follow-ups.md`) — biggest single effort. Once this lands, the rest
   becomes easier:
   - Embedded Tomcat replaces WAR.
   - `@Configuration` classes replace XML.
   - `Application.scala` replaces `web.xml`.
4. **JPA repositories** — port from Java `infrastructure/persistence/jpa/`.
5. **REST tracking** — port from Java
   `interfaces/tracking/ws/CargoTrackingRestService`. Use
   `spring-boot-starter-web` + Jackson; both are already on the
   classpath plan.
6. **CargoAdminController** — port from Java
   `interfaces/booking/web/`. Replaces our `CargoTrackingController`
   stub from PR #77.
7. **JMS infrastructure** — port last; only meaningful once you have a
   broker to point at.
8. **Acceptance tests** — Java uses page-object style with HtmlUnit /
   Selenium. Skip unless someone wants a UI test tier.

## Out of scope (for now)

- Drop-in Java compatibility. The Scala port isn't binary- or
  artifact-compatible with the Java reference, and shouldn't try to be.
  The goal is *pedagogical* parity (same DDD concepts, same flows), not
  bytecode parity.
- Diffing every business rule. The book defines the rules; both
  implementations should agree on them. If a port reveals a behavior
  difference, treat it as a bug-fix candidate — but that's part of
  individual test PRs, not this comparison.
