package se.citerus.dddsample.infrastructure.persistence.jpa

import java.util.UUID

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import org.springframework.transaction.annotation.Transactional

import se.citerus.dddsample.domain.model.cargo.{Cargo, CargoRepository, TrackingId}

/**
 * Adapter implementing the domain [[CargoRepository]] in terms of:
 *
 *   - the Spring Data [[CargoEntityRepository]] for CRUD
 *   - the [[CargoMapper]] for entity ↔ aggregate translation
 *
 * Mirrors the Magnum-PoC adapter shape, with two notable differences:
 *
 *   1. **Transactions via `@Transactional`** — the Spring proxy intercepts
 *      each method and opens / commits the transaction. Application
 *      services keep their own `@Transactional` boundary, which composes
 *      cleanly (propagation defaults to REQUIRED).
 *   2. **Refresh-by-mutation, not refresh-by-replace.** On `store`, we
 *      fetch the existing managed entity and mutate it via the mapper —
 *      `orphanRemoval=true` + cascade-all let Hibernate flush the right
 *      inserts/deletes for the legs collection.
 */
@org.springframework.stereotype.Repository
final class JpaCargoRepository(
    entityRepository: CargoEntityRepository,
    mapper: CargoMapper
) extends CargoRepository:

  @Transactional(readOnly = true)
  override def find(trackingId: TrackingId): Option[Cargo] =
    entityRepository.findByTrackingId(trackingId.idString).toScala.map(mapper.toAggregate)

  @Transactional(readOnly = true)
  override def getAll: List[Cargo] =
    entityRepository.findAll().asScala.toList.map(mapper.toAggregate)

  @Transactional
  override def store(cargo: Cargo): Unit =
    val entity = entityRepository
      .findByTrackingId(cargo.trackingId.idString)
      .orElseGet(() => new CargoEntity)
    mapper.applyTo(entity, cargo)
    entityRepository.save(entity)
    ()

  override def nextTrackingId(): TrackingId =
    TrackingId(UUID.randomUUID().toString.toUpperCase.substring(0, 10))
