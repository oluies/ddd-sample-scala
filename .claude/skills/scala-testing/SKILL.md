---
name: scala-testing
description: >-
  Testing conventions for this Scala DDD sample — ScalaTest 3.2 (`AnyFunSuite`,
  `AnyFlatSpec`, `should/Matchers`), MUnit, and ScalaCheck property-based
  tests. Covers the migration from ScalaTest 1.2 (currently on the classpath)
  to a Scala 3-compatible release, fixture conventions, mocking via
  Mockito-Scala, and integration tests against the Hibernate persistence
  layer. Use when writing or porting tests, or when a test is failing for a
  reason that looks framework-related rather than logic-related.
---

# scala-testing

The current `pom.xml` pins **ScalaTest 1.2**, which predates `Matchers`,
the `Any*Spec` package layout, and Scala 3 entirely. Any meaningful work on
tests should start by upgrading.

## Target stack

| Library             | Version           | Notes                                      |
| ------------------- | ----------------- | ------------------------------------------ |
| `scalatest_3`       | `3.2.19`          | Stable, Scala 3 native                     |
| `scalatestplus`     | `3.2.19.0`        | ScalaCheck integration                     |
| `scalacheck_3`      | `1.18.x`          | Property-based testing                     |
| `mockito-scala_3`   | `1.17.x` or `org.scalatestplus::mockito-5-12` | Mocks in Scala 3 |
| `munit_3`           | `1.0.x`           | Optional — lighter than ScalaTest          |

## Style for new tests

Default to `AnyFunSuite` + `Matchers`. It maps cleanly to a list of
`test("description") { ... }` blocks and reads well in DDD where each test
asserts one business rule.

```scala
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CargoTest extends AnyFunSuite with Matchers:

  test("a cargo without an itinerary is NOT_ROUTED") {
    val cargo = new Cargo(TrackingId("XYZ"), routeSpec)
    cargo.delivery.routingStatus shouldBe RoutingStatus.NotRouted
  }
```

For aggregates with rich invariants, an `AnyFlatSpec` with `behavior of`
groups reads better than a flat suite.

## Test layout convention

- Mirror the source package: `src/test/scala/se/citerus/dddsample/domain/model/cargo/CargoTest.scala`.
- One test class per production class — the sample already does this.
  Don't merge unrelated tests into "MiscTests".
- Slow / persistence tests go under
  `src/test/scala/se/citerus/dddsample/infrastructure/persistence/...` and
  carry `@Tag("integration")` or live under a `slow` suffix so they can be
  excluded from the fast loop.

## Property-based tests

DDD value objects are perfect for ScalaCheck. Generate a `TrackingId` and
assert round-trip and equality contracts:

```scala
import org.scalacheck.Gen
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TrackingIdProps extends AnyPropSpec with ScalaCheckPropertyChecks:
  val nonEmpty: Gen[String] = Gen.alphaNumStr.suchThat(_.nonEmpty)

  property("equality is structural") {
    forAll(nonEmpty) { s => TrackingId(s) shouldBe TrackingId(s) }
  }

  property("rejects empty input") {
    an[IllegalArgumentException] shouldBe thrownBy(TrackingId(""))
  }
```

## Mocking

The original Java sample leaned on Mockito for repositories in application-
service tests. In Scala 3 use **mockito-scala** (`org.mockito::mockito-scala`)
or `scalatestplus.mockito` — both support Scala 3.

Avoid mocking value objects and entities. Mock repositories and external
services only. If you find yourself stubbing return values for a
domain-object method, the test is at the wrong level — write a domain test
against real instances, not a service test with mocks.

## Hibernate / persistence integration tests

The sample's persistence tests load a Spring context and run against an
in-memory HSQLDB. Keep that pattern — it actually exercises the Hibernate
mappings, which is where most ORM bugs live. Don't replace these with
mock-based unit tests; that defeats the point.

```scala
@ContextConfiguration(Array("classpath:context-infrastructure-persistence.xml"))
class CargoRepositoryTest extends AnyFunSuite with Matchers
    with TestContextManager.Aware:
  // ...
```

## Running

Once the build is on Scala 3:

- `mvn test` — Maven runs ScalaTest via the surefire-style plugin.
- After moving to sbt: `sbt test` (fast) and `sbt "testOnly *RepositoryTest*"`.
- To exclude integration tests: tag them and configure exclusion in
  `pom.xml`/`build.sbt`.

## Migration checklist (ScalaTest 1.2 → 3.2)

1. Replace `import org.scalatest.{Spec, Suite}` with
   `import org.scalatest.funsuite.AnyFunSuite` (or chosen style).
2. Replace `extends Spec` with `extends AnyFunSuite`.
3. Replace `it("...")` with `test("...")`.
4. `import org.scalatest.matchers.should.Matchers` and add the trait.
5. Replace `should equal (x)` (1.2) with `shouldEqual x` or `shouldBe x`.
6. `expect(x) { actual }` → `actual shouldEqual x`.
7. `intercept[E] { ... }` is unchanged.
8. Replace deprecated `BeforeAndAfter` mixin paths with the current ones.

## Things to NOT do

- Don't re-introduce JUnit 4 alongside ScalaTest 3 — pick one runner. The
  sample already uses ScalaTest; keep it.
- Don't use `Thread.sleep` in tests. If you're tempted, the design needs a
  clock abstraction injected via `using Clock` — easy in Scala 3.
- Don't assert on `toString` output of domain objects. It will silently
  drift and cause flaky tests.
