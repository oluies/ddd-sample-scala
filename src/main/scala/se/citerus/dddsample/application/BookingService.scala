package se.citerus.dddsample.application

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.{Itinerary, TrackingId}
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.shared.DomainError

/**
 * Cargo booking service. Operations that can fail on the domain happy path
 * return `Either[DomainError, A]` rather than throwing — see
 * [[se.citerus.dddsample.domain.shared.DomainError]].
 */
trait BookingService:

  /** Registers a new (not yet routed) cargo. Returns the assigned tracking id. */
  def bookNewCargo(
      origin: UnLocode,
      destination: UnLocode,
      arrivalDeadline: Instant
  ): Either[DomainError, TrackingId]

  /** @return possible itineraries for this cargo, or `UnknownCargo` if none. */
  def requestPossibleRoutesForCargo(
      trackingId: TrackingId
  ): Either[DomainError, List[Itinerary]]

  def assignCargoToRoute(
      itinerary: Itinerary,
      trackingId: TrackingId
  ): Either[DomainError, Unit]

  /** Changes the destination of a cargo (origin and arrival deadline stay). */
  def changeDestination(
      trackingId: TrackingId,
      unLocode: UnLocode
  ): Either[DomainError, Unit]
