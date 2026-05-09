# Smoke test result — first run (2026-05-08)

**Status:** PARTIAL — agent stopped on a real `build.sbt` bug (correctly), but introduced 3 corruptions on the way that had to be repaired by hand.

**Model used:** local (Aider + Ollama, model unspecified — assume small/Q4-quantized).

## What the agent did

Edited 5 of the 8 files in `src/main/scala/se/citerus/dddsample/domain/shared/`:

- `AbstractSpecification.scala` — clean: stripped trailing `;`, removed bare `return`. Whole-file rewrite (line endings normalised).
- `AndSpecification.scala` — partially clean. **CORRUPTION:** truncated `spec2.isSatisfiedBy` → `spec2.isSatisfBy`.
- `NotSpecification.scala` — clean: removed bare `;` line, stripped trailing `;`.
- `OrSpecification.scala` — partially clean. **CORRUPTION:** truncated `spec2.isSatisfiedBy` → `spec2.isSatisfBy`.
- `Specification.scala` — partially clean. **CORRUPTION:** inserted `@int ` into a Javadoc comment (`{ @code this }` → `{ @int @code this }`).

Did **not** edit (correctly — they had no `;` to strip):
- `Entity.scala`
- `ValueObject.scala`
- `DomainEvent.scala`

Also did **not** write the required `SMOKETEST_RESULT.md` self-report — communicated only via chat.

## Out-of-bounds work (must-not-do violations)

The agent created **4 phantom duplicate files** at a misspelled path:

```
src/main/scala/se/criterus/dddsample/domain/shared/AbstractSpecification.scala
src/main/scala/se/criterus/dddsample/domain/shared/NotSpecification.scala
src/main/scala/se/criterus/dddsample/domain/shared/OrSpecification.scala
src/main/scala/se/criterus/dddsample/domain/shared/Specification.scala
```

Note `criterus` vs the correct `citerus`. The package declarations inside these files said `package se.citerus...` (correct) but the file system path was wrong. Removed.

## Why it stopped

`sbt` failed to load with `not found: value packageWar`. This was a real bug in `build.sbt` (I had referenced an xsbt-web-plugin 4.x key that doesn't exist). The agent correctly stopped per the prompt's STOP condition, rather than editing `build.sbt`. Fixed in this session.

## Repairs applied

1. Deleted the 4 phantom files at `src/main/scala/se/criterus/...`.
2. Fixed `Specification.scala` line 20: removed `@int ` from the comment.
3. Fixed `AndSpecification.scala` line 12: `isSatisfBy` → `isSatisfiedBy`.
4. Fixed `OrSpecification.scala` line 12: `isSatisfBy` → `isSatisfiedBy`.
5. Fixed `build.sbt`: removed bogus `packageWar` setting; tightened `bookingFacadeJar` task.

## Verdict on model fitness

Failure modes observed are characteristic of small / heavily-quantized models:
- **Identifier truncation under copy** (`isSatisfiedBy` → `isSatisfBy`).
- **Token hallucination in passthrough text** (`@int` in a comment).
- **Path drift** (`criterus` for `citerus`) when generating new file names from
  what should have been a verbatim copy.

For the remaining 12 batches (74 files), recommend:

1. Bump model size: `gemma4:31b` (Q4_K_M, ~20 GB) or `qwen2.5-coder:32b`.
2. Tighten Aider's edit set per batch — never pass directories, only the
   exact files in the batch.
3. After each batch, run `git status` and abort if any path outside the
   batch is touched, **or** any path contains an unfamiliar segment.
4. Diff every comment edit by hand — small models are more likely to
   hallucinate inside text the rule says to leave alone.

## Compile state (after repairs)

```
=== domain/shared/ errors ===
(none)
=== total errors ===
92
```

**`domain/shared/` is clean.** The remaining 92 errors are in packages that
have not been migrated yet (`domain/model/*`, `application/`,
`infrastructure/`, `interfaces/`, `com/pathfinder/`, `com/aggregator/`).
This is exactly the expected state after a successful batch-1 run.

**Smoke test verdict:** PASS, with caveats. The agent did the mechanical
work, but introduced enough corruption (3 hallucinations + 4 misplaced
files) that a human had to clean up. For the remaining 12 batches, use a
larger model or expect a similar repair cost per batch.
