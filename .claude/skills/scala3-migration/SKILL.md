---
name: scala3-migration
description: >-
  Migrate Scala 2 code to Scala 3. Covers the new control syntax, given/using
  contextual abstractions, enum, extension methods, opaque types, top-level
  definitions, union/intersection types, and the most common compile-error
  pitfalls (procedure syntax, infix types, structural types, package objects,
  early initializers, existential types, implicit conversions). Use when
  upgrading a Scala 2.x codebase to Scala 3, when porting a single file, or
  when interpreting `scalafix` / `-Xsource:3` / `-rewrite -source 3.0-migration`
  output. Tailored for this project's migration from Scala 2.8 to Scala 3.
---

# scala3-migration

This project is migrating from Scala 2.8 to Scala 3. The migration is being
tracked on `task007/scala3-*` branches. Use this skill whenever editing Scala
sources or build files during the migration.

## Migration order (do not skip steps)

1. **Bring Scala 2 forward first.** Jump 2.8 → 2.13 before attempting Scala 3.
   Scala 3 source-compatibility is defined against 2.13, not earlier minors.
2. **Compile under `-Xsource:3 -deprecation`** on 2.13 and fix every warning.
   This converts most syntax-level differences into errors with auto-fixes.
3. **Run `scalafix` rules** — `Scala3-migrate`, `ProcedureSyntax`,
   `ExplicitResultTypes`, `LeakingImplicitClassVal`, `NullaryOverride`.
4. **Switch the compiler to Scala 3** (`scala-library` → `scala3-library_3`,
   plugin → `dotty-maven-plugin` or move to sbt). Compile with
   `-source 3.0-migration -rewrite` to auto-rewrite the remaining mechanical
   differences. Commit those rewrites in their own commit.
5. **Drop `-source 3.0-migration`** and fix the residual errors by hand —
   these are the genuine semantic changes.

## Mechanical differences (auto-rewritten by `-source 3.0-migration -rewrite`)

| Scala 2                               | Scala 3                                     |
| ------------------------------------- | ------------------------------------------- |
| `def foo() { ... }` (procedure)       | `def foo(): Unit = { ... }`                 |
| `val x: T forSome { type T }`         | (existentials gone — refactor)              |
| `import a.b._`                        | `import a.b.*`                              |
| `import a.{b => c}`                   | `import a.{b as c}`                         |
| `xs: _*`                              | `xs*`                                       |
| `(_: Int) + 1` placeholder in lambdas | usually unchanged; some forms need explicit |
| `class C[+T] private[this] (...)`     | `private[this]` is gone; use `private`      |

## Semantic changes that need human attention

- **Implicits → givens.** `implicit val x: T = ...` becomes `given T = ...` (or
  `given name: T = ...`). `implicit def conv(a: A): B` becomes
  `given Conversion[A, B] = ...` *and* you must `import scala.language.implicitConversions`
  at the use site. Implicit parameter lists become `using` parameter lists.
- **Enums replace sealed-trait + case-object enumerations.** Prefer `enum
  Color { case Red, Green, Blue }`. Keep the sealed-trait form only when you
  need GADT-style refinement that `enum` cannot express.
- **Extension methods replace implicit classes.**
  `implicit class Foo(val s: String)` → `extension (s: String) def foo = ...`.
- **Opaque types replace `AnyVal` value classes** for zero-cost wrappers
  without boxing pitfalls — important for DDD value objects (CustomerId,
  TrackingId, etc.).
- **`@main` replaces `extends App`.** `extends App` still works but the body
  no longer runs in a static initializer the way it did in 2.x; tests of
  `App` objects often break. Prefer `@main def run(): Unit = ...`.
- **Package objects are deprecated** — move their members to top-level
  declarations in a regular `package` block.
- **Variance and override checking is stricter.** Many `override` keywords
  that 2.x silently inferred are now required.
- **Auto-tupling is gone** for `(a, b) => f` style — be explicit.
- **`null` is no longer `Nothing`** under explicit nulls (`-Yexplicit-nulls`),
  off by default but worth turning on for new code.

## Project-specific gotchas (DDD sample, Scala 2.8 → 3)

- **ScalaTest 1.2 is on the classpath.** That predates `Matchers`,
  `AnyFunSuite`, and the package layout. Plan to upgrade to ScalaTest 3.2.x in
  the same change — there is no Scala 3 build of 1.2. See `scala-testing`.
- **Java 8 collection-conversion implicits** (`JavaConversions`) were
  removed. Replace with `import scala.jdk.CollectionConverters.*` and
  `.asScala` / `.asJava`.
- **Spring + CXF reflection.** Several DDD entities are reflected on by
  Spring/CXF. Scala 3 case classes generate slightly different bytecode for
  `copy` / `apply` / synthetic accessors — verify with a smoke test that
  serialization still round-trips after the migration.
- **`maven-scala-plugin` is dead.** The Scala 3 path is either
  `scala-maven-plugin` (3.4.0+) with `<scalaVersion>3.x</scalaVersion>` or —
  preferably — migrate the build to sbt/Mill. Maven works but the toolchain
  around it (scalafix, scalafmt, metals) assumes sbt/Mill.

## Playbook for a single file

1. Read the file. Note every `implicit`, every procedure-syntax `def`, every
   `package object`, every `_*`, every `extends App`.
2. Apply mechanical rewrites first (the table above).
3. Convert implicits to givens / using / extension / Conversion as
   appropriate.
4. Compile. If errors remain, they are almost always one of: missing
   `using` at a call site, missing `import language.implicitConversions`,
   variance now explicit, or a structural-type / existential-type usage
   that has to be refactored.
5. Run the file's tests.

## What NOT to do

- Don't blanket-add `using` to every parameter list — it changes call-site
  syntax and breaks Java interop for that method.
- Don't convert every `sealed trait` + `case object` enumeration to `enum` if
  the codebase relies on the trait being extensible from another file (DDD
  bounded-context boundaries sometimes do this).
- Don't leave `-source 3.0-migration` on permanently — it masks real warnings.
