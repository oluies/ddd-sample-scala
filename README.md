# ddd-sample-scala

A Scala port of the [Citerus DDD Sample](https://github.com/citerus/dddsample-core)
— Eric Evans' canonical cargo-shipping example used to teach Domain-Driven
Design tactical patterns: entities, value objects, aggregates, repositories,
domain services, domain events, and specifications.

## Status

Migrated from **Scala 2.8** to **Scala 3.3.4 LTS** on `task007/scala3-15d0bf`.
The build is on **sbt 1.10** / **Java 17**. `sbt compile`, `sbt Test/compile`,
and `sbt test` are all green.

| Component        | Version              | Notes                                       |
| ---------------- | -------------------- | ------------------------------------------- |
| Scala            | 3.3.4 LTS            | dialect: `-source:3.3`                      |
| Build            | sbt 1.10             | Maven was removed mid-migration             |
| Java             | 17                   | required by Spring 5.2 / Hibernate 5.4 deps |
| Test framework   | ScalaTest 3.2.19     | + scalatestplus-mockito                     |
| Mocking          | mockito-scala 1.17   | EasyMock removed                            |
| Spring           | 5.2.19               | `javax.*` era — Spring 6 is a follow-up     |
| Hibernate        | 5.4.24               | XML mappings preserved                      |
| CXF (JAX-WS)     | 3.3.8                | for the booking facade                      |

See [`.claude/plans/scala3-upgrade.md`](.claude/plans/scala3-upgrade.md) for
the full plan and [Migration notes](#migration-notes-2026-05) below for what
was actually done.

## Layout

```
src/main/scala/se/citerus/dddsample/
  domain/{model,service,shared}/        ← framework-free DDD core
  application/                          ← use-case orchestration
  infrastructure/{persistence,routing}/ ← Hibernate, external services
  interfaces/{booking,handling,tracking}/  ← Spring MVC + CXF facades
```

The DDD invariant: `domain.*` never imports from `infrastructure.*` or
`interfaces.*`.

## Build & run

```bash
sbt compile                       # compile main sources
sbt test                          # run all tests against in-memory HSQLDB
sbt "testOnly *CargoTest"         # run a single suite
sbt scalafmtAll                   # format the codebase
sbt scalafmtCheckAll              # CI-style format check

# Web app (xsbt-web-plugin)
sbt "Jetty/start"                 # serve UI on :8080
sbt "Jetty/stop"
sbt package                       # build the WAR
sbt bookingFacadeJar              # secondary jar for the JAX-WS facade
```

Requires JDK 17 and sbt 1.10+ on the PATH. Install sbt with
`brew install sbt` or via [Coursier](https://get-coursier.io/).

## Dependency management

- **Dependabot** ([`.github/dependabot.yml`](.github/dependabot.yml)) —
  weekly sbt + GitHub Actions PRs, grouped by Spring / CXF / Hibernate /
  ActiveMQ / Jackson / logging / test frameworks. Defers `org.scala-lang*`
  to Scala Steward.
- **Scala Steward** ([`.scala-steward.conf`](.scala-steward.conf)) — enroll
  by adding `oluies/ddd-sample-scala` to your `scala-steward-repos` list;
  Steward picks up the repo-local config automatically.

## Working with Claude Code

Project-local skills live under `.claude/skills/`:

- **`scala3-migration`** — invoke when editing Scala sources during the
  Scala 2 → 3 migration.
- **`scala-ddd-tactical`** — invoke when adding to or refactoring the
  domain model.
- **`scala-testing`** — invoke when writing or porting tests.

`CLAUDE.md` at the repo root has the short briefing for the assistant.

## Reference

- Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of
  Software*, Addison-Wesley, 2003.
- [Citerus DDD Sample (Java)](https://github.com/citerus/dddsample-core).
- [Scala 3 migration guide](https://docs.scala-lang.org/scala3/guides/migration/compatibility-intro.html).

## Migration notes (2026-05)

The Scala 2.8 → 3.3 migration was done in 13 batches by package. Highlights
of what changed beyond mechanical syntax:

**Build**
- `pom.xml` deleted; `build.sbt` is the source of truth.
- xsbt-web-plugin 4.2.4 replaces the Maven WAR + Jetty plugin.
- Legacy `lib/scalatest-1.2-for-scala-2.8.0.RC3-SNAPSHOT.jar` removed (was an
  unmanaged dep that conflicted with ScalaTest 3.2.19).
- Spring's old monolithic `org.springframework:spring` artifact was split
  into `spring-core`, `spring-context`, `spring-beans`, `spring-aop`,
  `spring-tx`, `spring-orm`, `spring-jdbc`, `spring-jms`, `spring-web`,
  `spring-webmvc`.
- Strict scalac options (`-Wunused:imports`, `-Wvalue-discard`,
  `-Xfatal-warnings`) are temporarily disabled in `build.sbt`. A cleanup
  PR should re-enable them and remove the now-unused imports.

**Domain model — real Scala 3 changes (not just syntax)**
- `CarrierMovement` and `Leg` had constructor params that shadowed
  same-named methods (allowed in Scala 2, error in Scala 3). Params
  renamed to `_departureTime` / `_arrivalTime` / `_loadTime` /
  `_unloadTime` and made `private val`.
- `Itinerary.END_OF_DAYS` used the non-existent `Math.MAX_LONG` (latent
  bug masked by the old build) → fixed to `Long.MaxValue`.
- `HandlingHistory` used `List.sort(predicate)` (gone in Scala 2.13+) →
  `sortWith`.
- `Delivery` reduced its non-local `return` count; a few remain inside
  `for`/`while` loops and emit deprecation warnings (Scala 3 wants
  `boundary` / `boundary.break`). Behavior is unchanged.

**Library renames**
- `org.apache.commons.lang.*` → `org.apache.commons.lang3.*` (38 sites).
- `scala.reflect.BeanProperty` → `scala.beans.BeanProperty`.
- `scala.collection.JavaConversions` → `scala.jdk.CollectionConverters`.
- `org.hibernate.classic.Session` → `org.hibernate.Session`.

**Stubbed-out (need follow-up rewrite)**
- `interfaces.tracking.CargoTrackingController` extended Spring 2.x's
  removed `SimpleFormController` — class kept on the classpath as a stub
  with a TODO; request-handling logic needs to be ported to Spring 5
  `@Controller` / `@GetMapping`.
- `infrastructure.persistence.hibernate.AbstractRepositoryTest` and
  `CargoRepositoryTest` extended Spring 2.x's removed
  `AbstractTransactionalDataSourceSpringContextTests`. Stubbed with the
  original assertions preserved as TODO comments. Needs port to Spring 5
  TestContext (`SpringExtension` / `@SpringJUnitConfig`).

**Test rewrite**
- 3 unit tests (`BookingServiceTest`, `HandlingEventServiceTest`,
  `RouteSpecificationTest`) ported from JUnit 3 + EasyMock + ScalaTest 1.2
  to ScalaTest 3.2 `AnyFunSuite` + Mockito. **All 6 tests pass.**

**What's NOT done** (deliberately, in scope for follow-up PRs)
- Spring 5 → 6 (Jakarta EE).
- Hibernate 5 → 6.
- Java 21 baseline.
- Filling in `CargoTrackingController` and the integration tests.
- Re-enabling strict scalac flags + cleaning up unused imports.

## License

See [`license.txt`](license.txt) — original Citerus license preserved.
