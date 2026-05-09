# Smoke-test prompt — batch 1 only (`domain/shared/`)

> **Purpose:** Verify your local agent + tooling pipeline before turning it
> loose on the full 13-batch migration. This batch is the smallest and
> most mechanical: 8 tiny files, mostly stray-semicolon cleanup, no Scala 3
> features beyond syntax. If your agent can't pass this smoke test, the
> full migration won't go well.
>
> **How to use:** Paste everything below the `---` line as the agent's
> system / first user message. The agent runs to completion (or STOPs)
> and writes a result file you can inspect.

---

# Smoke test: migrate 8 files in `se.citerus.dddsample.domain.shared`

You will rewrite **8 small files** so they compile cleanly on **Scala 3.3.4**.
The build is already configured (`build.sbt` targets Scala 3.3.4 / Java 17
with sbt 1.10).

## Working directory

```
/Users/oluies/projects/ddd-sample-scala/.worktrees/task007/scala3-15d0bf
```

## The 8 files (do them in this order)

1. `src/main/scala/se/citerus/dddsample/domain/shared/Entity.scala`
2. `src/main/scala/se/citerus/dddsample/domain/shared/ValueObject.scala`
3. `src/main/scala/se/citerus/dddsample/domain/shared/DomainEvent.scala`
4. `src/main/scala/se/citerus/dddsample/domain/shared/Specification.scala`
5. `src/main/scala/se/citerus/dddsample/domain/shared/AbstractSpecification.scala`
6. `src/main/scala/se/citerus/dddsample/domain/shared/AndSpecification.scala`
7. `src/main/scala/se/citerus/dddsample/domain/shared/OrSpecification.scala`
8. `src/main/scala/se/citerus/dddsample/domain/shared/NotSpecification.scala`

## What to fix in each file (and ONLY this)

These are small files. The only syntactic incompatibilities are:

### Rule A — Bare `;` lines

After a `package` declaration, some files have a line that is just `;`:

```scala
package se.citerus.dddsample.domain.shared

;

/**
 * ...
 */
class AndSpecification...
```

**Action:** Delete that bare `;` line entirely (and any blank line padding
around it that becomes redundant).

### Rule B — Trailing `;` on statements

Many lines end in `;` (Java habit). Scala 3 accepts most of these but
they're noise. Remove a trailing `;` from any line where the `;` is the
last non-whitespace character.

Examples:

```scala
return new AndSpecification[T](this, specification);   →   new AndSpecification[T](this, specification)
def isSatisfiedBy(t: T): Boolean;                      →   def isSatisfiedBy(t: T): Boolean
```

While you're at it, remove the bare `return` keyword in front of single
expressions:

```scala
return new AndSpecification[T](this, specification);   →   new AndSpecification[T](this, specification)
return spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t);   →   spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t)
```

(Reason: Scala 3 flags `return` outside `def` bodies as a warning, and
single-expression method bodies don't need it. This is a one-line cleanup.)

### Rule C — Nothing else

These 8 files do **NOT** have:
- procedure syntax (`def foo() { ... }`)
- `import x._`
- `commons-lang` imports
- `JavaConversions`
- implicit declarations
- `extends App`

If you see any of those, **STOP** — you're editing the wrong file.

## What success looks like

After all 8 files are saved:

```bash
sbt -batch -no-colors compile 2>&1 | tail -50
```

The output should include `[success]`. Other parts of the codebase will
still fail to compile because they're not migrated yet — that is expected.
You're verifying that **your 8 files** don't introduce errors.

To check that specifically, look for errors mentioning paths under
`domain/shared/`. There should be **zero** such errors.

## Workflow

For each file in order:

1. Read it.
2. Apply Rule A (delete bare `;` lines).
3. Apply Rule B (strip trailing `;` and unnecessary `return`).
4. Save.

After file 8:

5. Run `sbt -batch -no-colors compile 2>&1 | tail -50`.
6. Filter for errors in `domain/shared/`. If any: read the first one, fix
   it, repeat from step 5.
7. Run `sbt -batch scalafmtAll`.
8. Write the result report (template below).
9. Do **NOT** `git commit` — let the human review first.

## Result report

Write your report to `.claude/handoff/SMOKETEST_RESULT.md` with this
**exact** structure:

```markdown
# Smoke test result

**Status:** PASS | FAIL | BLOCKED

## Files modified
- src/main/scala/se/citerus/dddsample/domain/shared/Entity.scala       (N edits)
- ... (one line per file, with the count of edits you made)

## Compile check
- Command: `sbt -batch -no-colors compile`
- Exit: 0 | non-zero
- Errors mentioning `domain/shared/`: <count>
- First 20 lines of error output (if any):
  ```
  ...paste here...
  ```

## scalafmt
- Command: `sbt -batch scalafmtAll`
- Exit: 0 | non-zero

## Notes
<one paragraph: anything surprising, anything you commented out, anything
the rules didn't cover>
```

## STOP conditions

- A file in the list is **missing** on disk.
- A file contains syntax not described above (procedure syntax, implicit
  declarations, etc.).
- The compile error references `domain/shared/` and you've tried to fix
  it three times without progress.
- `sbt` itself errors before reaching compilation (network, version
  resolution, plugin failure).

When you STOP, write the **same** report file but with `Status: BLOCKED`
and put the reason and last 20 lines of output in the Notes section.

## Reminders

- Do **not** edit `build.sbt` or anything under `project/`.
- Do **not** run `git commit`.
- Do **not** touch any file outside `domain/shared/`.
- Do **not** add new imports — these files don't need any.
- Do **not** "improve" the code (rename, reformat, refactor). Mechanical
  rewrites only.

If your local agent can pass this smoke test cleanly, it's ready for
`gemma-source-migration.md` (the full 13-batch migration). If not, the
report will tell us where the pipeline broke before we waste a
multi-hour run.
