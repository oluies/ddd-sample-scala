package se.citerus.dddsample.infrastructure.persistence.jpa

import scala.jdk.CollectionConverters.*

import se.citerus.dddsample.domain.model.cargo.*
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}

/**
 * Bidirectional mapping between mutable [[CargoEntity]] (JPA's world) and
 * the immutable [[Cargo]] aggregate (domain's world).
 *
 * The hardest direction is **row → aggregate**: we have only ids for
 * Voyage and Location in the row, so we resolve them via the other
 * domain repositories. `Delivery` is re-derived from the cargo's
 * handling history rather than persisted — same convention as the
 * Magnum PoC and the in-memory repo.
 */
final class CargoMapper(
    locationRepository: LocationRepository,
    voyageRepository: VoyageRepository,
    handlingEventRepository: se.citerus.dddsample.domain.model.handling.HandlingEventRepository
):

  // ---- Aggregate → Entity ------------------------------------------------

  /**
   * Apply the aggregate's state onto an *existing* entity (typically one
   * fetched by tracking id) or onto a fresh empty `CargoEntity` if the
   * cargo is new. Mutating an already-managed entity is JPA-idiomatic.
   */
  def applyTo(entity: CargoEntity, cargo: Cargo): CargoEntity =
    entity.trackingId = cargo.trackingId.idString
    entity.originUnLocode = cargo.origin.unLocode.idString
    entity.specDestinationUnLocode = cargo.routeSpecification.destination.unLocode.idString
    entity.specArrivalDeadline = cargo.routeSpecification.arrivalDeadline

    // Refresh legs by clearing + repopulating; orphanRemoval=true on the
    // @OneToMany means Hibernate issues deletes for the dropped legs and
    // inserts for the new ones in one flush.
    entity.legs.clear()
    cargo.itineraryOpt.foreach { itin =>
      itin.legs.zipWithIndex.foreach { case (leg, idx) =>
        val legEntity = new LegEntity
        legEntity.cargo = entity
        legEntity.legIndex = idx
        legEntity.voyageNumber = leg.voyage.voyageNumber.idString
        legEntity.loadUnLocode = leg.loadLocation.unLocode.idString
        legEntity.unloadUnLocode = leg.unloadLocation.unLocode.idString
        legEntity.loadTime = leg.loadTime
        legEntity.unloadTime = leg.unloadTime
        entity.legs.add(legEntity)
      }
    }
    entity

  /** Convenience: produce a fresh CargoEntity from a domain Cargo. */
  def toEntity(cargo: Cargo): CargoEntity = applyTo(new CargoEntity, cargo)

  // ---- Entity → Aggregate ------------------------------------------------

  def toAggregate(entity: CargoEntity): Cargo =
    val origin = locationRepository
      .find(UnLocode(entity.originUnLocode))
      .getOrElse(
        throw new IllegalStateException(s"Unknown origin UN/Locode: ${entity.originUnLocode}")
      )
    val destination = locationRepository
      .find(UnLocode(entity.specDestinationUnLocode))
      .getOrElse(
        throw new IllegalStateException(
          s"Unknown destination UN/Locode: ${entity.specDestinationUnLocode}"
        )
      )
    val spec     = RouteSpecification(origin, destination, entity.specArrivalDeadline)
    val withSpec = Cargo(TrackingId(entity.trackingId), spec)

    val withItinerary =
      if entity.legs.isEmpty then withSpec
      else
        val legs = entity.legs.asScala.toList.map { l =>
          Leg(
            voyageRepository
              .find(VoyageNumber(l.voyageNumber))
              .getOrElse(throw new IllegalStateException(s"Unknown voyage: ${l.voyageNumber}")),
            locationRepository
              .find(UnLocode(l.loadUnLocode))
              .getOrElse(throw new IllegalStateException(s"Unknown load loc: ${l.loadUnLocode}")),
            locationRepository
              .find(UnLocode(l.unloadUnLocode))
              .getOrElse(
                throw new IllegalStateException(s"Unknown unload loc: ${l.unloadUnLocode}")
              ),
            l.loadTime,
            l.unloadTime
          )
        }
        withSpec.assignToRoute(Itinerary(legs))

    val history =
      handlingEventRepository.lookupHandlingHistoryOfCargo(withItinerary.trackingId)
    withItinerary.deriveDeliveryProgress(history)
