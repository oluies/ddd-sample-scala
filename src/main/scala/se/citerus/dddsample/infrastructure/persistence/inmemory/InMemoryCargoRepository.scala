package se.citerus.dddsample.infrastructure.persistence.inmemory

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters.*

import se.citerus.dddsample.domain.model.cargo.{Cargo, CargoRepository, TrackingId}

/** In-memory [[CargoRepository]] — concurrent-safe via [[ConcurrentHashMap]].
  * Used by tests and by the lightweight startup profile that doesn't load
  * the full JPA stack (phase 9b).
  *
  * Tracking ids are minted from UUID strings; this matches the upstream
  * Hibernate adapter's behaviour close enough for the teaching example.
  */
final class InMemoryCargoRepository extends CargoRepository:

  private val cargos = new ConcurrentHashMap[String, Cargo]()

  override def find(trackingId: TrackingId): Option[Cargo] =
    Option(cargos.get(trackingId.idString))

  override def getAll: List[Cargo] = cargos.values.asScala.toList

  override def store(cargo: Cargo): Unit =
    cargos.put(cargo.trackingId.idString, cargo)

  override def nextTrackingId(): TrackingId =
    TrackingId(UUID.randomUUID().toString.toUpperCase.substring(0, 10))
