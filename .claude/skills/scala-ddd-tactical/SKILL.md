---
name: scala-ddd-tactical
description: >-
  Domain-Driven Design tactical patterns expressed idiomatically in Scala —
  Entities, Value Objects, Aggregates, Repositories, Domain Services, Domain
  Events, Specifications, and Factories. Tailored to the Citerus DDD cargo
  shipping sample (`se.citerus.dddsample.domain.*`). Use when adding to or
  refactoring the domain model, when porting Java DDD patterns to Scala, or
  when a value/entity/aggregate boundary is unclear.
---

# scala-ddd-tactical

This codebase is a Scala port of the Citerus DDD sample (the canonical Eric
Evans cargo-shipping example). The package layout follows DDD literally:

```
se.citerus.dddsample.domain.model.{cargo,handling,location,voyage}
se.citerus.dddsample.domain.service
se.citerus.dddsample.domain.shared      // Entity, ValueObject, Specification
se.citerus.dddsample.application        // application services (use cases)
se.citerus.dddsample.infrastructure     // persistence, routing, messaging
se.citerus.dddsample.interfaces         // booking / handling / tracking UIs + facades
```

Honor that boundary: domain code never imports from `infrastructure` or
`interfaces`. Application services orchestrate; they don't contain domain
logic.

## Pattern → Scala mapping

### Value Object

Immutable, identity-by-value, no setters. In Scala 3:

```scala
final case class TrackingId(id: String):
  require(id != null && id.nonEmpty, "TrackingId must be non-empty")
```

For a single-field wrapper that you want zero-cost and unboxed, prefer an
**opaque type** over a case class:

```scala
opaque type UnLocode = String
object UnLocode:
  def apply(s: String): UnLocode =
    require(s.matches("[A-Z]{5}"), s"Invalid UN/LOCODE: $s")
    s
  extension (u: UnLocode) def value: String = u
```

Equality and `hashCode` come for free with `case class`. With opaque types
they're inherited from the underlying type. Don't add custom equality — VO
equality must be structural.

### Entity

Identity-based equality. Override `equals`/`hashCode` on the id, not on the
fields:

```scala
abstract class Entity[ID]:
  def id: ID
  override def equals(o: Any): Boolean = o match
    case that: Entity[_] => this.getClass == that.getClass && this.id == that.id
    case _               => false
  override def hashCode: Int = id.hashCode
```

The existing `domain/shared/Entity.scala` already does this — keep entities
extending it.

### Aggregate

One root, one transactional boundary. External code holds references *only*
to the root. The root protects invariants of the whole graph.

In this sample:
- `Cargo` is an aggregate root (owns `Itinerary`, `Delivery`, `RouteSpecification`).
- `HandlingEvent` is an aggregate root (immutable, append-only).
- `Voyage` is an aggregate root (owns `Schedule`, `CarrierMovement`).
- `Location` is an aggregate root.

Repositories exist *only* per aggregate root — `CargoRepository`,
`VoyageRepository`, `LocationRepository`, `HandlingEventRepository`. Don't
add `ItineraryRepository`; you load the `Cargo` and traverse.

### Repository

Trait in `domain.model.<aggregate>`, implementation in `infrastructure.persistence`:

```scala
trait CargoRepository:
  def find(trackingId: TrackingId): Option[Cargo]
  def store(cargo: Cargo): Unit
  def nextTrackingId(): TrackingId
```

Return `Option[A]` rather than `null`. For collection results return
`List[A]`/`Seq[A]`, never `null`. Don't expose `Future[_]`/`IO[_]` from the
trait unless the whole stack is async — this codebase is currently synchronous.

### Domain Service

Belongs in `domain.service` only when the operation does not naturally live
on a single aggregate. `RoutingService` is the canonical example: it spans
`Cargo`, `Voyage`, `Location`. It is a `trait` with an `infrastructure`
implementation that calls the external pathfinder.

If you find yourself writing a "domain service" that mostly mutates one
aggregate, move the logic onto the aggregate root.

### Domain Event

Plain, immutable case class. Events are facts; past tense. Application
services publish them via `ApplicationEvents`:

```scala
final case class CargoWasMisdirected(cargo: Cargo)
final case class CargoHasArrived(cargo: Cargo)
```

Domain events are not the same as Spring/CXF messages — keep the domain
event type free of any framework annotations. Translation to a wire format
happens in `infrastructure`.

### Specification

`domain/shared/AbstractSpecification.scala` plus `And/Or/NotSpecification`
already implements the pattern. Use it for composable business rules
(`RouteSpecification.isSatisfiedBy(itinerary)`). Avoid scattering equivalent
boolean logic across application services.

### Factory

For non-trivial construction, put a factory method on the aggregate root's
companion object — *not* a separate `XFactory` class. Constructors stay
`private` only when an invariant must be enforced via the factory.

## Common mistakes to avoid

- **Anemic models.** A case class with no methods plus a service that
  manipulates it = anemic. Move the behavior onto the entity.
- **Leaking ORM annotations.** `@Entity`, `@Column`, `@ManyToOne` belong on
  Hibernate mapping (XML in this project) or in a separate persistence
  model. The domain model should compile without Hibernate on the classpath.
- **Public mutable collections on aggregates.** Return `Seq[A]` (immutable)
  or a defensive copy. Internal state can be `mutable.ArrayBuffer` but never
  expose it.
- **Cross-aggregate references by object.** Reference other aggregates by
  *id* (`TrackingId`, `UnLocode`, `VoyageNumber`), not by holding the
  object. This keeps aggregate boundaries crisp and avoids accidental
  large-graph loads.
- **Repositories returning entities outside their aggregate.**
  `CargoRepository` returns `Cargo`, period. Don't add a `findVoyageFor(...)`
  helper there.
- **Putting `RouteSpecification` invariants in the application service.**
  The application service decides *when* to check; the specification *is*
  the rule.

## When to reach for Scala 3 features here

- **Enums** for a closed set of states: `RoutingStatus`, `TransportStatus`,
  `HandlingActivityType`. Replaces the existing `sealed trait + case object`
  enumerations one-for-one.
- **Opaque types** for id wrappers (`TrackingId`, `UnLocode`,
  `VoyageNumber`) — eliminates allocation and `equals/hashCode` boilerplate.
- **Extension methods** instead of implicit conversions when adding helpers
  to `java.time.*` or to existing Java DDD types.
- **`given` + `using`** for crosscutting context (clock, id generator) that
  domain services need but you don't want to thread manually.

Don't reach for typeclasses (Cats, ZIO) in the domain layer of this sample
— it's deliberately framework-light. Save those for new bounded contexts.
