# Tests to add

**Created:** 2026-05-09
**Context:** The Scala 2.8 → 3.3 migration kept the existing 6 tests green
but didn't add coverage. This file lists the highest-value gaps so the next
test-PR can pick them up.

The migration's invariant was "behavior unchanged" — these tests would
turn that into a verifiable claim.

## Conventions for these tests

- ScalaTest 3.2 `AnyFunSuite` + `Matchers` (already on the classpath).
- Property tests via `org.scalatestplus::scalacheck-1-18` (already on the
  classpath).
- Mocks via `org.scalatestplus::mockito-5-12` + `org.mockito.Mockito.*`.
- Mirror the source package — one suite per production class, lives at
  `src/test/scala/<same-path>/<ClassName>Test.scala`.
- See `.claude/skills/scala-testing/SKILL.md` for the template.

---

## 1. `Delivery` — highest-priority gap

**File:** `src/test/scala/se/citerus/dddsample/domain/model/cargo/DeliveryTest.scala`
**Subject:** `domain/model/cargo/Delivery.scala`
**Why first:** This is the single class with the most business logic in
the codebase. It currently has zero tests. Every routing/tracking
regression would land here first.

**Suggested cases:**

- `transportStatus` for each `HandlingEventType`:
  - no events → `NOT_RECEIVED`
  - `LOAD` → `ONBOARD_CARRIER`
  - `UNLOAD` / `RECEIVE` / `CUSTOMS` → `IN_PORT`
  - `CLAIM` → `CLAIMED`
- `routingStatus`:
  - itinerary `null` → `NOT_ROUTED`
  - itinerary that satisfies the spec → `ROUTED`
  - itinerary that doesn't → `MISROUTED`
- `misdirected`:
  - last event is on the planned itinerary → `false`
  - last event is off-route → `true`
- `nextExpectedActivity` per branch in `calculateNextExpectedActivity`:
  - off-track → `None`
  - no last event → `Some(RECEIVE @ origin)`
  - last `LOAD` matched → `Some(UNLOAD @ unloadLocation)`
  - last `UNLOAD` mid-itinerary → next leg's `LOAD`
  - last `UNLOAD` final leg → `CLAIM`
  - last `RECEIVE` → first leg's `LOAD`
  - last `CLAIM` / `CUSTOMS` → `None`
- `unloadedAtDestination`:
  - last event is `UNLOAD` at destination → `true`
  - last event is `UNLOAD` somewhere else → `false`
  - last event is anything else → `false`

**Fixtures available:** `SampleLocations.*`, `SampleVoyages.*`, the
existing `RouteSpecificationTest` shows how to build itineraries quickly.

---

## 2. Value objects — `TrackingId`, `UnLocode`, `VoyageNumber`

**Files:**
- `src/test/scala/se/citerus/dddsample/domain/model/cargo/TrackingIdTest.scala`
- `src/test/scala/se/citerus/dddsample/domain/model/location/UnLocodeTest.scala`
- `src/test/scala/se/citerus/dddsample/domain/model/voyage/VoyageNumberTest.scala`

**Why:** These define the identity of every aggregate. Equality,
validation, and round-trip-through-`idString` are correctness-critical.

**Suggested cases (per VO):**

- Equality is by value (two instances with same id are `==`).
- `sameValueAs` agrees with `equals` on same-type comparisons.
- `hashCode` is consistent with `equals`.
- Constructor rejects null / empty (where applicable).
- `idString` round-trips: `new T(t.idString) == t`.

**Property test (UnLocode specifically):**

```scala
import org.scalacheck.Gen
val validLocode: Gen[String] =
  for
    country <- Gen.listOfN(2, Gen.alphaUpperChar).map(_.mkString)
    suffix  <- Gen.listOfN(3, Gen.oneOf(('A' to 'Z') ++ ('2' to '9'))).map(_.mkString)
  yield country + suffix

property("any 2-letter + 3-alphanum code is accepted") {
  forAll(validLocode) { s => new UnLocode(s).idString shouldBe s }
}

property("4-char or 6-char strings are rejected") {
  forAll(Gen.alphaUpperStr.filter(s => s.length == 4 || s.length == 6)) { s =>
    an[IllegalArgumentException] shouldBe thrownBy(new UnLocode(s))
  }
}
```

---

## 3. Specification composers

**File:** `src/test/scala/se/citerus/dddsample/domain/shared/SpecificationTest.scala`

**Subject:** `AbstractSpecification`, `AndSpecification`, `OrSpecification`,
`NotSpecification`.

**Why:** Composition is the whole point of the Specification pattern, and
it had a real bug during this migration (`isSatisfBy` truncation that the
local model introduced). A truth-table test would have caught it.

**Suggested cases:**

```scala
class AlwaysTrue  extends AbstractSpecification[Any]:
  def isSatisfiedBy(t: Any) = true
class AlwaysFalse extends AbstractSpecification[Any]:
  def isSatisfiedBy(t: Any) = false

// AND truth table
test("T && T = T") { (T and T).isSatisfiedBy(()) shouldBe true }
test("T && F = F") { (T and F).isSatisfiedBy(()) shouldBe false }
test("F && T = F") { (F and T).isSatisfiedBy(()) shouldBe false }
test("F && F = F") { (F and F).isSatisfiedBy(()) shouldBe false }

// OR / NOT analogously
```

---

## 4. `Itinerary.isExpected`

**File:** `src/test/scala/se/citerus/dddsample/domain/model/cargo/ItineraryTest.scala`

**Why:** `isExpected` decides whether handling events are misdirected.
Branches in `Itinerary.scala` per `HandlingEventType`:

- `RECEIVE` — first leg's load location matches event location.
- `LOAD` — some leg has matching `(loadLocation, voyage)`.
- `UNLOAD` — some leg has matching `(unloadLocation, voyage)`.
- `CLAIM` — last leg's unload location matches event location.
- `CUSTOMS` — always `true`.

Plus `legs.isEmpty → true`.

Also worth covering: `initialDepartureLocation`, `finalArrivalLocation`,
`finalArrivalDate`, `lastLeg` for empty / single / many leg itineraries.

---

## 5. `HandlingHistory`

**File:** `src/test/scala/se/citerus/dddsample/domain/model/handling/HandlingHistoryTest.scala`

**Why:** `mostRecentlyCompletedEvent` is the input to `Delivery`'s state
calculation. Ordering bugs here ripple everywhere.

**Suggested cases:**

- Empty history → `mostRecentlyCompletedEvent == None`.
- Single event → `Some(event)`.
- Multiple events → returns the one with the latest `completionTime`.
- `distinctEventsByCompletionTime` returns events sorted ascending by
  `completionTime`.

Note: the original code had a subtle bug —
`s.completionTime.compareTo(s.completionTime)` (twice `s`, never `t`).
The migration fixed this to `s.completionTime.compareTo(t.completionTime)`.
A test for ordering would have caught the original.

---

## 6. `HandlingEventFactory` — exception paths

**File:** add cases to existing
`src/test/scala/se/citerus/dddsample/application/HandlingEventServiceTest.scala`
or a new `HandlingEventFactoryTest.scala`.

**Why:** All three "not found" exceptions have specific business meaning.

**Suggested cases:**

- `cargoRepository.find(unknown) → None` → `UnknownCargoException`.
- `voyageRepository.find(unknown) → None` → `UnknownVoyageException`.
- `locationRepository.find(unknown) → None` → `UnknownLocationException`.
- Successful happy path: returns `HandlingEvent` with all the right fields.
- Voyage parameter `null` is allowed (`RECEIVE` / `CLAIM` / `CUSTOMS`
  events) — no `UnknownVoyageException` thrown.

---

## 7. `Cargo` — aggregate root

**File:** `src/test/scala/se/citerus/dddsample/domain/model/cargo/CargoTest.scala`

**Why:** `Cargo` is the central aggregate. Currently has no direct tests.

**Suggested cases:**

- Construction: rejects null `trackingId`, null `routeSpecification`.
- `origin` is derived from initial route specification.
- `assignToRoute` updates `itinerary` and recomputes `delivery`.
- `specifyNewRoute` updates the spec and recomputes `delivery`.
- `deriveDeliveryProgress` updates `delivery` based on handling history.
- `sameIdentityAs` is by `trackingId`, not by attributes.

---

## 8. Hibernate integration tests (LARGER effort)

**Files:**
- `src/test/scala/se/citerus/dddsample/infrastructure/persistence/hibernate/AbstractRepositoryTest.scala`
  (currently a stub)
- `src/test/scala/se/citerus/dddsample/infrastructure/persistence/hibernate/CargoRepositoryTest.scala`
  (currently a stub)

**Why:** These exercise Hibernate XML mappings against HSQLDB. They catch
ORM regressions that mock-based tests can't. The original 6 cases are
preserved as TODO comments inside the stub files.

**What's needed:**

Port `AbstractRepositoryTest` from Spring 2.x's removed
`AbstractTransactionalDataSourceSpringContextTests` to Spring 5's
`SpringExtension` model:

```scala
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.{ContextConfiguration, junit.jupiter.SpringExtension}
import org.springframework.transaction.annotation.Transactional

@ExtendWith(Array(classOf[SpringExtension]))
@ContextConfiguration(Array("classpath:context-infrastructure-persistence.xml", "classpath:context-domain.xml"))
@Transactional
abstract class AbstractRepositoryTest extends AnyFunSuite { ... }
```

Then port the 6 `CargoRepositoryTest` cases listed in the stub:
`testFindByCargoId`, `testFindByCargoIdUnknownId`, `testSave`,
`testReplaceItinerary`, `testFindAll`, `testNextTrackingId`.

This is a separate, focused PR — not a quick add.

---

## Suggested rollout

- **Quick wins (one PR):** items 2 (value objects), 3 (specifications),
  5 (HandlingHistory). All small, all property-testable, all unblock CI
  trust quickly.
- **Medium PR:** item 1 (Delivery) — biggest behavioral coverage win.
- **Medium PR:** items 4 (Itinerary), 6 (HandlingEventFactory exceptions),
  7 (Cargo aggregate).
- **Large, separate PR:** item 8 (Hibernate integration tests, Spring 5
  TestContext port).
