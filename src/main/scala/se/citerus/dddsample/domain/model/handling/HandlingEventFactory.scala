package se.citerus.dddsample.domain.model.handling

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}

/**
 * Application-tier factory that creates [[HandlingEvent]]s, looking up the
 * referenced cargo / voyage / location via their repositories.
 *
 * Returns `Either[CannotCreateHandlingEventException, HandlingEvent]` — the
 * lookup failures (`UnknownCargoException`, `UnknownVoyageException`,
 * `UnknownLocationException`) and the `HandlingEvent.apply` validation
 * failure all surface as `Left` values. Callers at the boundary
 * (`HandlingEventServiceImpl`) decide whether to translate them back into
 * thrown exceptions for transactional rollback.
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
  ): Either[CannotCreateHandlingEventException, HandlingEvent] =
    for
      cargo    <- cargoRepository.find(trackingId).toRight(UnknownCargoException(trackingId))
      location <- locationRepository.find(unlocode).toRight(UnknownLocationException(unlocode))
      voyage <- voyageNumber.fold(
        Right(None): Either[CannotCreateHandlingEventException, Option[
          se.citerus.dddsample.domain.model.voyage.Voyage
        ]]
      )(vn => voyageRepository.find(vn).map(Some(_)).toRight(UnknownVoyageException(vn)))
      event <- buildEvent(cargo, completionTime, registrationTime, eventType, location, voyage)
    yield event

  private def buildEvent(
      cargo: se.citerus.dddsample.domain.model.cargo.Cargo,
      completionTime: Instant,
      registrationTime: Instant,
      eventType: HandlingEventType,
      location: se.citerus.dddsample.domain.model.location.Location,
      voyage: Option[se.citerus.dddsample.domain.model.voyage.Voyage]
  ): Either[CannotCreateHandlingEventException, HandlingEvent] =
    try
      Right(voyage match
        case Some(v) =>
          HandlingEvent(cargo, completionTime, registrationTime, eventType, location, v)
        case None =>
          HandlingEvent(cargo, completionTime, registrationTime, eventType, location)
      )
    catch case e: Exception => Left(new CannotCreateHandlingEventException(e))
