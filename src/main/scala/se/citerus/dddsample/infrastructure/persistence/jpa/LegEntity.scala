package se.citerus.dddsample.infrastructure.persistence.jpa

import jakarta.persistence.{
  Column,
  Entity,
  FetchType,
  GeneratedValue,
  GenerationType,
  Id,
  JoinColumn,
  ManyToOne,
  Table
}
import java.time.Instant

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

/**
 * JPA entity for one entry in a Cargo's `Itinerary`.
 *
 * Owned by [[CargoEntity]] via a bidirectional `@OneToMany` / `@ManyToOne`.
 * The back-reference is required by Hibernate for the orphan-removal +
 * cascade-all relationship that mirrors aggregate ownership.
 */
@Entity
@Table(name = "leg")
class LegEntity:

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @BeanProperty
  var id: java.lang.Long = uninitialized

  // The parent in the aggregate. LAZY because we always traverse from the
  // Cargo down, never the other way.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cargo_id", nullable = false)
  @BeanProperty
  var cargo: CargoEntity = uninitialized

  @Column(name = "leg_index", nullable = false)
  @BeanProperty
  var legIndex: Int = 0

  @Column(name = "voyage_number", nullable = false)
  @BeanProperty
  var voyageNumber: String = uninitialized

  @Column(name = "load_un_locode", nullable = false)
  @BeanProperty
  var loadUnLocode: String = uninitialized

  @Column(name = "unload_un_locode", nullable = false)
  @BeanProperty
  var unloadUnLocode: String = uninitialized

  @Column(name = "load_time", nullable = false)
  @BeanProperty
  var loadTime: Instant = uninitialized

  @Column(name = "unload_time", nullable = false)
  @BeanProperty
  var unloadTime: Instant = uninitialized
