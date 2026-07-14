package se.citerus.dddsample.infrastructure.persistence.jpa

import jakarta.persistence.{
  CascadeType,
  Column,
  Entity,
  FetchType,
  GeneratedValue,
  GenerationType,
  Id,
  OneToMany,
  OrderBy,
  Table
}
import java.time.Instant

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

/**
 * JPA entity mirroring the [[se.citerus.dddsample.domain.model.cargo.Cargo]]
 * aggregate.
 *
 * Pure persistence model — separate from the domain entity per D1. Lives
 * with a Scala-shaped-like-Java surface (mutable `var` fields, public
 * no-arg constructor, JPA annotations) because JPA's reflection-based
 * machinery requires it. The mapper class [[CargoMapper]] converts this
 * row to/from the immutable
 * [[se.citerus.dddsample.domain.model.cargo.Cargo]].
 *
 * Cross-aggregate references stored by **id** (UN/Locode strings, voyage
 * number strings) rather than `@ManyToOne` to other JPA entities. This
 * keeps the JPA model focused on Cargo's own aggregate boundary and
 * avoids the lazy-loading proxy spaghetti you get from cross-aggregate
 * `@ManyToOne(fetch = LAZY)`.
 */
@Entity
@Table(name = "cargo")
class CargoEntity:

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @BeanProperty
  var id: java.lang.Long = uninitialized

  @Column(name = "tracking_id", unique = true, nullable = false)
  @BeanProperty
  var trackingId: String = uninitialized

  @Column(name = "origin_un_locode", nullable = false)
  @BeanProperty
  var originUnLocode: String = uninitialized

  @Column(name = "spec_destination_un_locode", nullable = false)
  @BeanProperty
  var specDestinationUnLocode: String = uninitialized

  @Column(name = "spec_arrival_deadline", nullable = false)
  @BeanProperty
  var specArrivalDeadline: Instant = uninitialized

  // Aggregate-owned child collection. Cascade.ALL + orphanRemoval=true means
  // the cargo's lifecycle owns its legs (no orphans, no manual leg deletes).
  //
  // `targetEntity = classOf[LegEntity]` is set explicitly because Scala 3's
  // bytecode for `@BeanProperty`-derived accessors on a parameterised
  // `java.util.List[LegEntity]` field doesn't always preserve the type
  // parameter for Hibernate's reflection lookup — without this hint,
  // Hibernate sees a raw `List` and aborts with an AnnotationException.
  @OneToMany(
    mappedBy = "cargo",
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    orphanRemoval = true,
    targetEntity = classOf[LegEntity]
  )
  @OrderBy("legIndex ASC")
  @BeanProperty
  var legs: java.util.List[LegEntity] = new java.util.ArrayList[LegEntity]()
