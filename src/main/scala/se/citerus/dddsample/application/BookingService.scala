package se.citerus.dddsample.application

import java.util.Date
import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.cargo.Itinerary

/**
 * Cargo booking service.
 */
trait BookingService {

  /**
   * Registers a new cargo in the tracking system, not yet routed.
   *
   * @param origin cargo origin
   * @param destination cargo destination
   * @param arrivalDeadline arrival deadline
   * @return Cargo tracking id
   */
  def bookNewCargo(origin: UnLocode, destination: UnLocode, arrivalDeadline: Date): TrackingId

  /**
   * Requests a list of itineraries describing possible routes for this cargo.
   *
   * @param trackingId cargo tracking id
   * @return A list of possible itineraries for this cargo
   */
  def requestPossibleRoutesForCargo(trackingId: TrackingId): List[Itinerary]

  /**
   * @param itinerary itinerary describing the selected route
   * @param trackingId cargo tracking id
   */
  def assignCargoToRoute(itinerary: Itinerary, trackingId: TrackingId): Unit

  /**
   * Changes the destination of a cargo.
   *
   * @param trackingId cargo tracking id
   * @param unLocode UN locode of new destination
   */
  def changeDestination(trackingId: TrackingId, unLocode: UnLocode): Unit

}