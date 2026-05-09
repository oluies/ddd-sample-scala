# Handoff prompt — Scala 2 → 3 source migration (for a local Gemma-class agent)

> **How to use:** Feed everything below the `---` line as the system / first
> user message to your local agent. It is self-contained: file paths,
> rewrite rules, batch order, verification commands, and a STOP condition
> are all explicit. Don't expect the model to fill in gaps from training —
> if a step is missing, treat it as out-of-scope.

> Tested target: a small instruction-tuned coding model (Gemma-class).
> Keep its context filled with **one batch's files at a time**, not the
> whole repo.

---

# Your job

You are migrating a Scala 2.8 codebase to **Scala 3.3.4**. The build is
already configured: `build.sbt` targets Scala 3.3.4 / Java 17 with sbt 1.10.
Sources still use Scala 2.8 syntax. **Your only job is to rewrite the
sources so `sbt compile` and `sbt test` pass.**

You do **NOT**:
- Add features.
- Change library versions in `build.sbt`.
- Edit `pom.xml` (it has been deleted).
- Refactor for "improvement" — only fix what does not compile.
- Touch Hibernate XML files (`*.hbm.xml`) or Spring XML files unless a
  rename of a Scala class forces it.

# Working directory

```
/Users/oluies/projects/ddd-sample-scala/.worktrees/task007/scala3-15d0bf
```

# Required reading before you start

1. `.claude/plans/scala3-source-migration.md` — the per-batch plan and the
   inventory of what's in the codebase.
2. `.claude/skills/scala3-migration/SKILL.md` — playbook for Scala 2 → 3
   syntax differences.
3. `build.sbt` — read once. Note the ScalaTest / mockito / commons-lang3
   versions. Do not change it.
4. `CLAUDE.md` — the repo's house rules.

# Operating rules

1. **One batch at a time.** Batches are listed in
   `.claude/plans/scala3-source-migration.md` under "Top-level packages".
   Start at batch 1 (`domain/shared/`) and do not move to batch 2 until
   batch 1 compiles.
2. **One file at a time within a batch.** Read the file, apply the
   mechanical rules, fix the rest, save.
3. **Compile after every batch**, not every file:
   ```bash
   sbt -batch -no-colors compile 2>&1 | tail -200
   ```
   If output ends with `[success]`, the batch is green — commit and move on.
   Otherwise, fix the top error and recompile.
4. **Do not delete code you don't understand.** Comment it out with
   `// FIXME(scala3-migration): <one-line reason>` and continue. Report
   commented-out blocks at the end.
5. **Do not edit `build.sbt` or `project/*` files.** If a missing
   dependency seems to be the problem, STOP and report.
6. **Format only at end of batch:**
   ```bash
   sbt -batch scalafmtAll
   ```
7. **Commit per batch** with this message format:
   ```
   migrate(<batch>): Scala 2 → 3 syntax
   ```
   Example: `migrate(domain/shared): Scala 2 → 3 syntax`.

# Mechanical rewrite rules (apply per file, in order)

Apply these as a first pass. They cover ~80% of the work. Do **NOT**
hand-format — `scalafmt` runs at end of batch.

| ID  | Find                                            | Replace                                       |
| --- | ----------------------------------------------- | --------------------------------------------- |
| R1  | trailing `;` after package/import/statement     | (delete the `;`)                              |
| R2  | `import x.y._`                                  | `import x.y.*`                                |
| R3  | `import x.{a => b}`                             | `import x.{a as b}`                           |
| R4  | `xs: _*`  (varargs splat)                       | `xs*`                                         |
| R5  | `def foo() { body }` (procedure)                | `def foo(): Unit = { body }`                  |
| R6  | `import scala.reflect.BeanProperty`             | `import scala.beans.BeanProperty`             |
| R7  | `import scala.collection.JavaConversions._`     | `import scala.jdk.CollectionConverters.*`     |
| R8  | `org.apache.commons.lang.` (38 sites)           | `org.apache.commons.lang3.`                   |
|     | (this is a **package rename only**; class names stay the same: `Validate`, `StringUtils`, `ObjectUtils`, `builder.*`) |
| R9  | `import org.scalatest.junit.AssertionsForJUnit` | (delete the line — test rewrite handles it)   |
| R10 | `import org.scalatest.mock.EasyMockSugar`       | `import org.scalatestplus.mockito.MockitoSugar` |

Important about R7: the rule rewrites the import but at call sites you may
also need to add `.asScala` or `.asJava` to convert Java↔Scala collections.
Only the compiler will tell you which call sites need it. Add the call when
it errors, not preemptively.

# Test-file rewrite (5 files in `src/test/scala/**`)

Tests use a JUnit 4 + EasyMock + ScalaTest 1.x mix that does **not** exist
on the new classpath at all. You must rewrite each test class as a
ScalaTest 3.2 `AnyFunSuite`.

Replace this header block:

```scala
import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.EasyMockSugar

class FooTest extends TestCase with AssertionsForJUnit with EasyMockSugar {
```

with this header block:

```scala
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*

class FooTest extends AnyFunSuite with Matchers with MockitoSugar:
```

(Note the colon — Scala 3 optional braces syntax. The closing `}` at end
of class becomes a blank line at the same indentation level.)

Then, inside the body:

| Old                                       | New                                  |
| ----------------------------------------- | ------------------------------------ |
| `def testFoo() = { ... }`                 | `test("foo") { ... }`                |
| `assertEquals(expected, actual)`          | `actual shouldEqual expected`        |
| `assertTrue(cond)`                        | `cond shouldBe true`                 |
| `assertFalse(cond)`                       | `cond shouldBe false`                |
| `assertNull(x)`                           | `x shouldBe null`                    |
| `assertNotNull(x)`                        | `x should not be null`               |
| `expect(mock.x()).andReturn(y)`           | `when(mock.x()).thenReturn(y)`       |
| `replay(...)` / `verify(...)`             | (delete; Mockito doesn't need them)  |
| `mock[Foo]`                               | `mock[Foo]` (unchanged — `MockitoSugar` provides it) |

If a test is currently empty (`def testFoo() = { }`), keep it empty —
`test("foo") { }` is a valid no-op test.

# Special case: `Cargo.scala` is malformed in the working tree

The file currently begins:

```scala
package se.citerus.dddsample.domain.model.cargo

;

import org.apache.commons.lang.Validate;
...

class Cargo(
  def routeSpecification = { mutableRouteSpecification }
```

The constructor parameter list is **not closed** before the methods begin
— this won't compile in any Scala version. The intended constructor
parameters can be inferred from the rest of the class body and from the
upstream Java original:
<https://github.com/citerus/dddsample-core/blob/master/dddsample/src/main/java/se/citerus/dddsample/domain/model/cargo/Cargo.java>

Likely shape (verify before applying):

```scala
class Cargo(
  trackingId: TrackingId,
  routeSpec: RouteSpecification
) extends Entity[TrackingId]:
  ...
```

If you cannot determine the correct shape, STOP and report the file as
blocked.

# Per-file workflow

For each file in the current batch:

1. **Read it** in full.
2. **Apply the mechanical rules** R1–R10 (search-and-replace within the
   file).
3. **Save and continue** to the next file in the batch. Don't compile yet.

After the last file in the batch:

4. Run:
   ```bash
   sbt -batch -no-colors compile 2>&1 | tail -200
   ```
5. If `[success]`: run `sbt -batch scalafmtAll`, then commit:
   ```bash
   git add -A && git commit -m "migrate(<batch>): Scala 2 → 3 syntax"
   ```
6. If errors: read the **first** error, fix the file it points to, repeat.
   Do not chase later errors first — they are usually consequences of the
   first one.

# Verification at the end

When all 13 batches are done, run all of:

```bash
sbt -batch -no-colors compile      # must end [success]
sbt -batch -no-colors Test/compile # must end [success]
sbt -batch -no-colors test         # tests must run; failures are bugs to fix
sbt -batch scalafmtCheckAll        # must end [success]
```

Then write a short summary report (under 200 words) listing:
- Batches completed.
- Any files commented-out with `// FIXME(scala3-migration)`.
- Any non-mechanical refactors you made and why.
- Any tests that fail with their assertion message.

# STOP conditions (do not "be helpful" — STOP and report)

- A `build.sbt` change is the only way forward.
- A library you need is missing from `libraryDependencies`.
- A file is structurally malformed beyond the `Cargo.scala` case described
  above.
- A test asserts on `toString` output you can't match.
- A Hibernate mapping or Spring XML rename would be needed.
- The same compile error reappears after three fix attempts.

When stopping, write the reason to `.claude/handoff/STOP.md` with:
- Batch and file you were on.
- The exact command you last ran.
- The first 50 lines of its output.
- One sentence on what you think is needed.

# Final reminder

You are doing a **mechanical port**, not a redesign. When in doubt, prefer
the smallest possible change that makes the compiler happy. The point of
the migration is to land Scala 3 with behaviour unchanged.
