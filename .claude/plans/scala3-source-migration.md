# Scala 2.8 → 3.3 source migration plan

**Companion to:** `.claude/plans/scala3-upgrade.md` (strategic plan)
**Scope:** Make `sbt compile` and `sbt test` pass on Scala 3.3.4.
**Build state:** `build.sbt` already targets Scala 3.3.4 / Java 17. Sources
are still Scala 2.8 syntax. Only the **sources** are in scope here.
**Last updated:** 2026-05-08

## Inventory (measured 2026-05-08)

| Item                                            | Count |
| ----------------------------------------------- | ----- |
| Main `.scala` files                             | 74    |
| Test `.scala` files                             | 5     |
| `import x._` (→ `.*`)                           | 52    |
| `org.apache.commons.lang.*` legacy package      | 38    |
| `scala.reflect.BeanProperty` / `@BeanProperty`  | 6     |
| `scala.collection.JavaConversions`              | 2     |
| Procedure-syntax `def foo() { ... }`            | 2     |
| `package object` files                          | 0     |
| `implicit val/def/class/object` declarations    | 0     |
| EasyMock imports (test-only)                    | 3     |

The codebase is **mechanically-rewritable for the most part** — there are no
implicits, no package objects, and no Scala 2 enumerations. The hard work
is the test rewrite and any Spring/Hibernate/CXF reflection fallout.

## Top-level packages (work batches)

The migration is sliced by package. Each batch ends at a green
`sbt compile` for the files in that batch (or a clean failure pointing at
something out of scope).

```
1. domain/shared/                              ← foundations: Entity, ValueObject, Specification
2. domain/model/location/                      ← UnLocode, Location, SampleLocations
3. domain/model/voyage/                        ← VoyageNumber, Voyage, Schedule
4. domain/model/cargo/                         ← TrackingId, Cargo, RouteSpecification, Itinerary
5. domain/model/handling/                      ← HandlingEvent, HandlingHistory
6. domain/service/                             ← RoutingService
7. application/ + application/impl/ + util/    ← BookingService, CargoInspectionService
8. infrastructure/persistence/hibernate/       ← *Repository implementations
9. infrastructure/routing/                     ← ExternalRoutingService
10. interfaces/booking/ + handling/ + tracking/ ← Spring MVC + CXF facades
11. com/pathfinder/                            ← graph traversal
12. com/aggregator/                            ← inbound aggregator
13. src/test/scala/**                          ← 5 test files
```

Order matters: `domain/shared/` defines `Entity` and `ValueObject` that the
rest of the model extends. Location/voyage/cargo come next because they're
referenced from elsewhere. Tests come last because their fixtures depend on
production code being green.

## Mechanical rewrite rules

These are the rules that apply across the whole codebase. Apply them as a
first pass per file, then handle whatever remains.

| ID  | Rule                                                                                              |
| --- | ------------------------------------------------------------------------------------------------- |
| R1  | Drop stray Java-style semicolons after `package`, `import`, and statement ends — Scala 3 dislikes them in some positions. |
| R2  | `import x.y._` → `import x.y.*`                                                                   |
| R3  | `import x.{a => b}` → `import x.{a as b}`                                                         |
| R4  | `xs: _*` → `xs*` (varargs forwarding)                                                             |
| R5  | `def foo() { body }` (procedure) → `def foo(): Unit = { body }`                                   |
| R6  | `import scala.reflect.BeanProperty` → `import scala.beans.BeanProperty`                           |
| R7  | `import scala.collection.JavaConversions._` → `import scala.jdk.CollectionConverters.*` and add `.asScala` / `.asJava` at call sites |
| R8  | `import org.apache.commons.lang.*` → `import org.apache.commons.lang3.*` (38 sites; package only, classnames unchanged for the ones used here: `Validate`, `StringUtils`, `ObjectUtils`, `builder.*`) |
| R9  | `extends App` (if any) → `@main def run(): Unit = ...` *only if it's a top-level entry point*; leave class-extending-App alone if it's a Spring-loaded singleton. |
| R10 | Final-method overriding rules tightened — add `override` keywords the compiler asks for; do not remove existing ones. |

## Test-file rewrite (5 files)

Tests use a JUnit 4 + EasyMock + ScalaTest 1.x mix. Replace with ScalaTest 3.2:

| Old                                         | New                                                  |
| ------------------------------------------- | ---------------------------------------------------- |
| `extends TestCase with AssertionsForJUnit with EasyMockSugar` | `extends AnyFunSuite with Matchers with MockitoSugar` |
| `import junit.framework.TestCase`           | (delete)                                             |
| `import junit.framework.Assert._`           | (delete)                                             |
| `import org.easymock.EasyMock._`            | `import org.mockito.Mockito.*`                       |
| `import org.scalatest.junit.AssertionsForJUnit` | (delete)                                         |
| `import org.scalatest.mock.EasyMockSugar`   | `import org.scalatestplus.mockito.MockitoSugar`      |
| `def testFoo() = { ... }`                   | `test("foo") { ... }`                                |
| `assertEquals(expected, actual)`            | `actual shouldEqual expected`                        |
| `assertTrue(cond)`                          | `cond shouldBe true`                                 |
| EasyMock `expect(mock.x()).andReturn(y)`    | `when(mock.x()).thenReturn(y)`                       |
| EasyMock `replay(...)` / `verify(...)`      | (delete — Mockito doesn't need them)                 |

ScalaTest imports for new test classes:

```scala
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
```

## Gotchas the agent must watch for

1. **`Cargo.scala` is currently malformed** — its constructor parameter list
   appears unclosed in the working tree (`class Cargo(` followed directly by
   method defs). Reconstruct the constructor params from the rest of the
   class and the Java original at <https://github.com/citerus/dddsample-core>.
   Do not just paper over compile errors there.
2. **Spring XML bean refs use string class names.** When renaming a class, also
   grep `src/main/webapp/WEB-INF/*.xml` and `src/main/resources/**/*.xml` for
   the FQCN.
3. **Hibernate hbm.xml mapping files** under `src/main/resources/` reference
   class names. Same rule as Spring XML.
4. **CXF JAX-WS** annotations must remain on the booking-facade classes
   (`interfaces/booking/facade/`) — those are bytecode-load by reflection.
5. **`@BeanProperty`** (`scala.beans.BeanProperty`) is needed wherever Spring
   or Hibernate accesses a Scala field via Java getters/setters. Do not
   remove existing uses.
6. **`commons-lang` → `commons-lang3`** is a *package* rename only. The
   class names (`Validate`, `StringUtils`, `ObjectUtils`, the `builder` sub-
   package) are stable. `Validate.notNull` etc. all still exist.
7. **Don't reorder imports unless scalafmt asks** — the agent should run
   `sbt scalafmtAll` at the end of each batch, not hand-format.

## Per-batch checklist

For each batch in the order above:

1. List the files in the batch (`find <pkg> -name '*.scala'`).
2. For each file:
   a. Apply mechanical rules R1–R10 with grep-replace.
   b. Compile only that file (`sbt "scalac src/main/scala/<path>"` is not a
      thing in sbt; instead `sbt compile` and filter to errors in this batch).
   c. Fix the residual errors by hand.
3. After the batch, run `sbt scalafmtAll` to normalise formatting.
4. Commit with message `migrate: <batch name> to Scala 3`.

## Exit criteria

- [ ] `sbt compile` exits 0.
- [ ] `sbt Test/compile` exits 0.
- [ ] `sbt test` exits 0 (tests may need stubs but should not fail).
- [ ] `sbt scalafmtCheckAll` exits 0.
- [ ] CI workflow on the branch is green.
- [ ] No `-source:3.0-migration` flag in `build.sbt`.
- [ ] Smoke test: `sbt "Jetty/start"` boots the web app on `:8080` without
      a Spring context-load exception.

## Out of scope (explicit)

- New features.
- Switching from Spring 5 to Spring 6 / Jakarta EE.
- Switching from Hibernate 5 to Hibernate 6.
- Switching from XML Hibernate mappings to annotations.
- Changing the persistence model.
- Replacing `org.springframework:spring` (already done in `build.sbt`).
- Removing `easymock` deps (already done — they aren't in `build.sbt`).

These are tracked in `scala3-upgrade.md` Phase 4/5 and will be done after
this migration lands.
