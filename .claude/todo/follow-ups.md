# Migration follow-ups

**Created:** 2026-05-09
**Context:** The Scala 2.8 → 3.3 migration landed on `task007/scala3-15d0bf`
with everything compiling and the existing tests passing. These are the
known items deliberately deferred. Each is a separate PR.

---

## A. Restore strict scalac flags

**Where:** `build.sbt`

```scala
// Currently commented out — re-enable post-cleanup:
// "-Wunused:imports",
// "-Wvalue-discard",
// "-Xfatal-warnings",
```

**What:** During migration these were turned off because the old code had
~33 unused-import warnings (heritage from over-imported Java-style heads)
and a few `-Wvalue-discard` hits. Re-enabling them now would block any
forward progress.

**Steps:**
1. Re-enable `-Wunused:imports`. Run `sbt compile` and remove the unused
   imports it flags. Most are duplicates inserted during the original
   Java-to-Scala port.
2. Re-enable `-Wvalue-discard`. Add explicit `: Unit` returns or `val _ =`
   bindings where Scala 3 complains.
3. Re-enable `-Xfatal-warnings`. Build should be clean by now.

**Effort:** small (1–2 hours).

---

## B. Replace non-local `return` in `Delivery`

**Where:** `src/main/scala/se/citerus/dddsample/domain/model/cargo/Delivery.scala`

**What:** A few `return` statements remain inside `for`/`while` loops in
`calculateNextExpectedActivity`. Scala 3 deprecates non-local returns in
favor of `scala.util.boundary` / `boundary.break`. Build emits 10
deprecation warnings here today.

**Steps:** rewrite the two loops in `calculateNextExpectedActivity` using
`boundary { ... }` / `break(value)` or convert to `xs.collectFirst` /
`xs.iterator.takeWhile(...).find(...)` patterns. Add unit tests first
(see `tests-to-add.md` item 1) so any behavior change is caught.

**Effort:** small (1 hour, mostly because of the test prerequisite).

---

## C. Port `CargoTrackingController` to Spring 5

**Where:** `src/main/scala/se/citerus/dddsample/interfaces/tracking/CargoTrackingController.scala`

**What:** Currently a stub. The original extended Spring 2.x's
`SimpleFormController` (removed in Spring 3.0). Needs port to Spring 5's
`@Controller` + `@GetMapping` / `@PostMapping`.

**Original behavior (preserved as TODO comments in the stub):**
1. Find a `Cargo` by tracking id from the request.
2. Look up handling history.
3. Wrap in `CargoTrackingViewAdapter` and bind to the form view.
4. If not found, reject `trackingId` field with `cargo.unknown_id`
   message.

**Effort:** medium (4–8 hours, including a smoke test through Jetty).

---

## D. Port Hibernate integration tests to Spring 5 TestContext

**Where:**
- `src/test/scala/se/citerus/dddsample/infrastructure/persistence/hibernate/AbstractRepositoryTest.scala`
- `src/test/scala/se/citerus/dddsample/infrastructure/persistence/hibernate/CargoRepositoryTest.scala`

See `tests-to-add.md` item 8 for the detailed plan. Effort: medium-large.

---

## E. Spring 6 / Hibernate 6 / Java 21 (separate, optional)

**Why deferred:** the migration goal was Scala 3, not Jakarta EE. Bumping
Spring 5.2 → 6.x flips the world from `javax.*` to `jakarta.*` — every
Spring XML, every `web.xml`, every `javax.servlet` import. This is the
right call but a focused effort of its own.

**Steps when ready:**
1. Bump Spring to 6.1.x. Run `find src -name "*.scala" -o -name "*.xml" \
   | xargs sed -i '' -e 's|javax\.servlet|jakarta.servlet|g'`. Same for
   `javax.persistence`, `javax.transaction`, `javax.jms`, `javax.xml`.
2. Bump Hibernate to 6.4.x — query API changed; check HQL strings and
   `Session` usage.
3. Bump CXF to 4.0.x — also Jakarta.
4. Bump Java baseline to 21 (or 25, after testing).
5. `web.xml` switches from servlet 4.0 to 6.0; revisit Jetty version.

**Effort:** large (1–2 weeks). See `.claude/plans/scala3-upgrade.md`
"Phase 4" for the original write-up.

---

## F. Replace `org.apache.commons.logging` with SLF4J

**Where:** various — `BookingServiceImpl`, `CargoInspectionServiceImpl`,
`HandlingEventServiceImpl`, `BookingServiceFacadeImpl`, `ExternalRoutingService`.

**What:** All of these use `org.apache.commons.logging.LogFactory.getLog`.
The codebase already has SLF4J + jcl-over-slf4j on the classpath, so it
works — but it's an indirection. New idiom would be:

```scala
import org.slf4j.LoggerFactory
private val logger = LoggerFactory.getLogger(getClass)
```

**Effort:** trivial (30 minutes), purely cosmetic — no behavior change.
Roll into a cleanup PR.

---

## G. Convert id wrappers to opaque types

**Where:** `TrackingId`, `UnLocode`, `VoyageNumber`.

**What:** Currently `class T(val id: String) extends ValueObject[T]`.
Scala 3 opaque types eliminate the wrapper allocation and the
boilerplate `equals` / `hashCode` overrides:

```scala
opaque type TrackingId = String
object TrackingId:
  def apply(s: String): TrackingId =
    require(s != null && s.nonEmpty); s
  extension (id: TrackingId) def idString: String = id
```

**Effort:** medium (per type, ~1 hour, plus updating call sites). Worth
doing only after the value-object tests in `tests-to-add.md` item 2 land
— they pin the equality contract that the opaque-type rewrite has to
preserve.

**Trade-off:** opaque types lose pattern-match destructuring and don't
play as nicely with reflection-based frameworks (Hibernate, CXF).
Verify the booking-facade SOAP serialization still works before
switching.

---

## Suggested order

1. **A** (re-enable strict warnings) — small, foundational, prevents drift.
2. Quick test wins from `tests-to-add.md` (items 2, 3, 5).
3. **F** (commons-logging → SLF4J) — trivial cosmetic.
4. Medium tests (items 1, 4, 6, 7).
5. **B** (Delivery returns) — depends on item 1 tests.
6. **C** (CargoTrackingController) — independent.
7. **D** (Hibernate integration tests) — independent, larger.
8. **G** (opaque id types) — after items in `tests-to-add.md` land.
9. **E** (Spring 6 / Jakarta) — last, biggest, optional.
