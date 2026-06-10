package se.citerus.dddsample.domain.model.handling

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}
import se.citerus.dddsample.domain.shared.DomainError

/**
 * Application-tier factory that creates [[HandlingEvent]]s, looking up the
 * referenced cargo / voyage / location via their repositories.
 *
 * Returns `Either[DomainError, HandlingEvent]` — repository lookups surface
 * as `UnknownCargo` / `UnknownLocation` / `UnknownVoyage`, and the
 * `HandlingEvent.apply` validation failures (e.g. missing voyage for LOAD)
 * surface as `InvariantViolation`.
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
  ): Either[DomainError, HandlingEvent] =
    for
      cargo    <- cargoRepository.find(trackingId).toRight(DomainError.UnknownCargo(trackingId))
      location <- locationRepository.find(unlocode).toRight(DomainError.UnknownLocation(unlocode))
      voyage <- voyageNumber.fold(
        Right(None): Either[DomainError, Option[
          se.citerus.dddsample.domain.model.voyage.Voyage
        ]]
      )(vn => voyageRepository.find(vn).map(Some(_)).toRight(DomainError.UnknownVoyage(vn)))
      event <- buildEvent(cargo, completionTime, registrationTime, eventType, location, voyage)
    yield event

  private def buildEvent(
      cargo: se.citerus.dddsample.domain.model.cargo.Cargo,
      completionTime: Instant,
      registrationTime: Instant,
      eventType: HandlingEventType,
      location: se.citerus.dddsample.domain.model.location.Location,
      voyage: Option[se.citerus.dddsample.domain.model.voyage.Voyage]
  ): Either[DomainError, HandlingEvent] =
    try
      Right(voyage match
        case Some(v) =>
          HandlingEvent(cargo, completionTime, registrationTime, eventType, location, v)
        case None =>
          HandlingEvent(cargo, completionTime, registrationTime, eventType, location)
      )
    catch case e: IllegalArgumentException => Left(DomainError.fromThrowable(e))
