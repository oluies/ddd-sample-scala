package se.citerus.dddsample.domain.model.handling

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}

/**
 * Application-tier factory that creates [[HandlingEvent]]s, looking up the
 * referenced cargo / voyage / location via their repositories.
 *
 * Throws [[UnknownCargoException]] / [[UnknownVoyageException]] /
 * [[UnknownLocationException]] for missing referents; wraps any other
 * exception in [[CannotCreateHandlingEventException]].
 *
 * Upstream Java declares the checked exception in the throws clause. Scala
 * has no checked exceptions, so we omit the annotation.
 */
final class HandlingEventFactory(
    cargoRepository: CargoRepository,
    voyageRepository: VoyageRepository,
    locationRepository: LocationRepository
):

  def createHandlingEvent(
      registrationTime: Instant,
      completionTime: Instant,
      trackingId: TrackingId,
      voyageNumber: Option[VoyageNumber],
      unlocode: UnLocode,
      eventType: HandlingEventType
  ): HandlingEvent =
    val cargo =
      cargoRepository.find(trackingId).getOrElse(throw new UnknownCargoException(trackingId))
    val location =
      locationRepository.find(unlocode).getOrElse(throw new UnknownLocationException(unlocode))
    val voyage = voyageNumber.map { vn =>
      voyageRepository.find(vn).getOrElse(throw new UnknownVoyageException(vn))
    }

    try
      voyage match
        case Some(v) =>
          HandlingEvent(cargo, completionTime, registrationTime, eventType, location, v)
        case None => HandlingEvent(cargo, completionTime, registrationTime, eventType, location)
    catch case e: Exception => throw new CannotCreateHandlingEventException(e)
