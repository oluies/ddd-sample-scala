package se.citerus.dddsample.application

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.{Itinerary, TrackingId}
import se.citerus.dddsample.domain.model.location.UnLocode

/** Cargo booking service. */
trait BookingService:

  /** Registers a new (not yet routed) cargo. Returns the assigned tracking id. */
  def bookNewCargo(origin: UnLocode, destination: UnLocode, arrivalDeadline: Instant): TrackingId

  /** @return possible itineraries for this cargo. */
  def requestPossibleRoutesForCargo(trackingId: TrackingId): List[Itinerary]

  def assignCargoToRoute(itinerary: Itinerary, trackingId: TrackingId): Unit

  /** Changes the destination of a cargo (origin and arrival deadline stay). */
  def changeDestination(trackingId: TrackingId, unLocode: UnLocode): Unit
