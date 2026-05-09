# Scala 3 upgrade plan — ddd-sample-scala

**Status:** In progress, branch `task007/scala3-15d0bf`
**Author:** Örjan Lundberg
**Last updated:** 2026-05-08

> **Course change (2026-05-08):** The build was switched directly from Maven
> to sbt with Scala 3.3.4 + Java 17 as the target — phases 1 and 2 of the
> original plan were collapsed. Sources are still Scala 2.8 syntax;
> `sbt compile` is currently failing. The remaining work is the Scala 2 → 3
> source migration (originally Phase 3) without an intermediate 2.13 stop.
> The phases below are kept as historical record; the **only live phase is
> Phase 3**.

## Starting state

- Scala **2.8.0.RC3** (`pom.xml:97`) — released 2010, predates 2.13 by ~9 years.
- Maven build via the abandoned `org.scala-tools:maven-scala-plugin`
  (`pom.xml:79-90`).
- ScalaTest **1.2** (`pom.xml:100-103`) — predates `Matchers` and the
  `Any*Spec` package layout.
- Java target **1.6** (`pom.xml:33-34`).
- Spring 5.2.19 (modern), Hibernate 5.4.24 (modern), CXF 3.3.8 (modern) — the
  Java-side dependencies are fine.
- ActiveMQ 5.6.0, EasyMock 2.4, javassist 3.8 — old; will replace.

The Scala side is effectively a museum piece; the Java side is current. The
plan handles them separately.

## Target state

- **Scala 3.3.x LTS** (long-term-support line — currently 3.3.4).
- **Java 17** (Spring 6 requirement when we eventually move; 17 also
  unblocks records, sealed, pattern matching in Java tests).
- Build tool: **sbt 1.10.x** with the `sbt-dotty`-era machinery built in.
  Migration from Maven is part of this plan because the Scala 3 toolchain
  (scalafix, scalafmt, metals, scala-steward) targets sbt first.
- ScalaTest 3.2.19, ScalaCheck 1.18, mockito-scala (replace EasyMock).
- Logging: SLF4J 2.0.x + Logback 1.5.x (replace `slf4j-log4j12`).

## Strategy: small steps that each ship green

Five phases. Each ends with `mvn test` (or `sbt test` after phase 3) green
on `master`. Don't merge a phase that requires a follow-up to compile.

### Phase 0 — pre-flight (1 day)

- [ ] Pin Java to **8** in `pom.xml` (`<source>1.8</source>`) — needed for
      anything beyond Scala 2.8. Run tests, merge.
- [ ] Replace `maven-scala-plugin` with `net.alchim31.maven:scala-maven-plugin`
      (4.9.x) at the **same Scala 2.8 version**. Run tests, merge. This
      isolates the build-tool change from the language change.
- [ ] Add `.github/dependabot.yml` (already drafted) and enroll in
      Scala Steward (already drafted `.scala-steward.conf`).
- [ ] Add a CI workflow (`.github/workflows/ci.yml`) running `mvn test` on
      push/PR. Migrations without CI are reckless.

### Phase 1 — Scala 2.8 → 2.13 (1–2 weeks, biggest single jump)

This is the hard one. Every other phase is mechanical.

- [ ] Bump `scala-library` to **2.13.14** in `pom.xml`.
- [ ] Bump `scalatest` to **3.2.19** (this also crosses the 1.2 → 3.x
      boundary). Update test imports per the `scala-testing` skill checklist.
- [ ] Replace EasyMock with **mockito-scala** 1.17.x.
- [ ] Replace `scala.collection.JavaConversions` with
      `scala.jdk.CollectionConverters.*` everywhere.
- [ ] Fix every compile error. The bulk are: procedure syntax, `_*`,
      `withFilter`, removed `Actor` API (none used here, good), parser
      combinators moved to `scala-parser-combinators` (none used).
- [ ] Bump `slf4j-*` to 1.7.36 (last 1.x). Logback comes in phase 4.
- [ ] Replace `commons-lang` 2.3 with `commons-lang3` 3.14 (different
      package: `org.apache.commons.lang` → `org.apache.commons.lang3`).
- [ ] Replace `hsqldb` 1.8.0.7 with `org.hsqldb:hsqldb` 2.7.x (group id
      changed). Verify Hibernate-mapping integration tests still pass.
- [ ] Replace ActiveMQ 5.6.0 with **5.18.x** (or move to Artemis).
- [ ] Drop `javassist` from direct deps — Hibernate brings its own.
- [ ] Compile with `-Xsource:3 -deprecation` and clean every warning.
      This is the gating step before Scala 3.

**Exit criteria:** `mvn test` green on Scala 2.13.14 with `-Xsource:3` and
**zero deprecation warnings**.

### Phase 2 — Maven → sbt (3–5 days)

- [ ] Add `build.sbt`, `project/build.properties` (sbt 1.10.x),
      `project/plugins.sbt` (sbt-scalafix, sbt-scalafmt).
- [ ] Replicate Maven dependencies in sbt. Spring/Hibernate/CXF stay; this
      is a Scala source build, not a packaging change.
- [ ] Replicate the WAR packaging via `sbt-native-packager` or keep the
      Maven build alongside until phase 5.
- [ ] Replicate the booking-facade jar (`pom.xml:48-65`) via an sbt
      sub-project.
- [ ] Run `sbt test` — must match `mvn test` exactly. Diff the two test
      reports.

**Why now?** Doing this *before* Scala 3 means the Scala-3-specific
tooling (scalafix `Scala3-migrate`, `-rewrite -source 3.0-migration`)
runs through the toolchain it was designed for.

### Phase 3 — Scala 2.13 → Scala 3.3 (1 week)

Follow the `scala3-migration` skill exactly. Highlights:

- [ ] Run `sbt scalafixAll` with `Scala3-migrate`, `ProcedureSyntax`,
      `ExplicitResultTypes`, `LeakingImplicitClassVal`. Commit per rule.
- [ ] Switch `scalaVersion` to `3.3.4`.
- [ ] Compile with `-source 3.0-migration -rewrite`. Commit the auto-rewrite
      diff in **one commit** so it can be reviewed quickly.
- [ ] Drop `-source 3.0-migration`. Fix the residual errors by hand
      (implicits → givens, structural types, `extends App`, package objects).
- [ ] Convert id wrappers (`TrackingId`, `UnLocode`, `VoyageNumber`,
      `CarrierMovementId`, `CargoRoutingId`) from `case class` to **opaque
      type** for zero-allocation. Single commit per id type.
- [ ] Convert closed enumerations (`RoutingStatus`, `TransportStatus`,
      `HandlingActivityType`) to Scala 3 `enum`. Single commit per enum.
- [ ] Re-run all tests. Spring/CXF reflection on case classes is the
      highest risk — verify XML round-trips for the booking-facade DTOs.

**Exit criteria:** `sbt test` green on Scala 3.3.4 with no
`-source 3.0-migration` flag and no deprecation warnings.

### Phase 4 — Java 17 + Spring 6 (optional, 3–5 days)

Decoupled from the Scala migration; do it after phase 3 is merged.

- [ ] Bump Java to 17 (`javacOptions += "--release:17"`).
- [ ] Bump Spring 5.2 → 6.1 (`org.springframework` → still
      `org.springframework`, but `javax.*` → `jakarta.*` is the breaking
      change). Update `web.xml`, Spring XML, and any `javax.servlet` imports.
- [ ] Bump CXF 3.3 → 4.0 (also Jakarta).
- [ ] Bump Hibernate 5.4 → 6.4 (Jakarta + new query API).

This phase is large and largely orthogonal to Scala. Land Scala 3 first
so the rollback story is small.

### Phase 5 — cleanup (1 day)

- [ ] Remove the Maven build if sbt has been the source of truth for ≥2
      weeks with no fallback usage.
- [ ] Re-enable Scala Steward bumps for `scala3-library` minor versions.
- [ ] Add `scalafmt` + pre-commit hook.
- [ ] Update README and CLAUDE.md to drop "migration in progress" notes.

## Risks & rollback

| Risk                                                         | Mitigation                                              |
| ------------------------------------------------------------ | ------------------------------------------------------- |
| Hibernate XML mappings break on case-class bytecode shape    | Keep an integration test that loads + saves every       |
|                                                              | aggregate via the real ORM in HSQLDB.                   |
| CXF JAX-WS marshalling breaks on Scala 3 case classes        | Smoke test in phase 1 (Scala 2.13) and phase 3.         |
| Spring XML autowiring broken by package-object removal       | Grep for `package object` after phase 3 and rewrite.    |
| Big phase-1 jump conflicts with concurrent feature work      | Freeze main during phase 1; communicate dates.          |
| Scalafix `Scala3-migrate` fails on Spring/CXF-annotated code | Run rule-by-rule, not all at once. Skip and hand-port.  |

Rollback is per-phase: each phase is one or more PRs to `master` that
build green. Reverting the merge commit reverts the phase.

## Sequencing summary

```
phase 0  (CI, dependabot, build-plugin swap)         ~1d
phase 1  (Scala 2.8 → 2.13 + ScalaTest 3.2)          1–2w   ← biggest jump
phase 2  (Maven → sbt)                               3–5d
phase 3  (Scala 2.13 → Scala 3.3)                    1w
phase 4  (Java 17 + Spring 6, optional)              3–5d
phase 5  (cleanup)                                   1d
```

Total: roughly **3–5 weeks** of focused work, deliverable in 5 PRs (one
per phase). Phase 4 can be deferred indefinitely without blocking the
Scala 3 outcome.

## Open questions

1. Do we keep WAR packaging? If we're modernizing, an embedded servlet
   (Jetty 12 / Tomcat 10) via `sbt-native-packager` is simpler.
2. Do we keep CXF SOAP, or replace the booking facade with REST + JSON?
   The DDD sample is a teaching artifact; SOAP is part of its history but
   not its lesson.
3. Do we keep Spring, or move new bounded contexts to a lighter DI (e.g.
   `cats.effect.IO` resources, manual wiring)? This is a teaching repo,
   so probably "keep Spring, don't fight the original".
