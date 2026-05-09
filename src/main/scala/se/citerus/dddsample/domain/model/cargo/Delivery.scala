package se.citerus.dddsample.domain.model.cargo

import java.util.Date

import org.apache.commons.lang3.Validate
import org.apache.commons.lang3.builder.EqualsBuilder

import se.citerus.dddsample.domain.model.handling.*
import se.citerus.dddsample.domain.model.handling.HandlingEvent
import se.citerus.dddsample.domain.model.handling.HandlingHistory
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * The actual transportation of the cargo, as opposed to
 * the customer requirement (RouteSpecification) and the plan (Itinerary).
 */
class Delivery private (
    val lastEvent: Option[HandlingEvent],
    itinerary: Itinerary,
    routeSpecification: RouteSpecification
) extends ValueObject[Delivery] {
  val calculatedAt                     = new Date()
  val transportStatus: TransportStatus = calculateTransportStatus
  val misdirected                      = calculateMisdirectionStatus(itinerary)
  val routingStatus                    = calculateRoutingStatus(itinerary, routeSpecification)
  val lastKnownLocation                = calculateLastKnownLocation
  val currentVoyage                    = calculateCurrentVoyage
  val eta                              = calculateEta(itinerary)
  val nextExpectedActivity  = calculateNextExpectedActivity(routeSpecification, itinerary)
  val unloadedAtDestination = calculateUnloadedAtDestination(routeSpecification)

  def calculateTransportStatus: TransportStatus =
    lastEvent match {
      case None => NOT_RECEIVED
      case Some(event) =>
        event.eventType match {
          case LOAD    => ONBOARD_CARRIER
          case UNLOAD  => IN_PORT
          case RECEIVE => IN_PORT
          case CUSTOMS => IN_PORT
          case CLAIM   => CLAIMED
        }
    }

  private def calculateMisdirectionStatus(itinerary: Itinerary): Boolean =
    lastEvent match {
      case None        => false
      case Some(event) => !itinerary.isExpected(event)
    }

  private def calculateRoutingStatus(
      itinerary: Itinerary,
      routeSpecification: RouteSpecification
  ): RoutingStatus =
    if (itinerary == null) {
      NOT_ROUTED
    } else {
      if (routeSpecification.isSatisfiedBy(itinerary)) ROUTED
      else MISROUTED
    }

  private def calculateLastKnownLocation: Option[Location] =
    lastEvent.map(_.location)

  private def calculateCurrentVoyage: Option[Voyage] =
    lastEvent match {
      case Some(event) if transportStatus.equals(ONBOARD_CARRIER) => Some(event.voyage)
      case _                                                      => None
    }

  private def onTrack: Boolean =
    routingStatus.equals(ROUTED) && !misdirected

  private def calculateEta(itinerary: Itinerary): Option[Date] =
    if (onTrack) Some(itinerary.finalArrivalDate())
    else None

  private def calculateNextExpectedActivity(
      routeSpecification: RouteSpecification,
      itinerary: Itinerary
  ): Option[HandlingActivity] = {
    if (!onTrack) {
      return None
    }

    val event = lastEvent.getOrElse {
      return Some(new HandlingActivity(RECEIVE, routeSpecification.origin))
    }

    event.eventType match {
      case LOAD =>
        itinerary.legs.foreach { leg =>
          if (leg.loadLocation.sameIdentityAs(event.location)) {
            return Some(HandlingActivity(UNLOAD, leg.unloadLocation, Some(leg.voyage)))
          }
        }
        None
      case UNLOAD =>
        val it = itinerary.legs.iterator
        while (it.hasNext) {
          val leg = it.next()
          if (leg.unloadLocation.sameIdentityAs(event.location)) {
            if (it.hasNext) {
              val nextLeg = it.next()
              return Some(HandlingActivity(LOAD, nextLeg.loadLocation, Some(nextLeg.voyage)))
            } else {
              return Some(HandlingActivity(CLAIM, leg.unloadLocation))
            }
          }
        }
        None
      case RECEIVE =>
        val firstLeg = itinerary.legs(0)
        Some(HandlingActivity(LOAD, firstLeg.loadLocation, Some(firstLeg.voyage)))
      case CLAIM   => None
      case CUSTOMS => None
    }
  }

  private def calculateUnloadedAtDestination(routeSpecification: RouteSpecification): Boolean =
    lastEvent match {
      case None => false
      case Some(event) =>
        UNLOAD.sameValueAs(event.eventType) &&
        routeSpecification.destination.sameIdentityAs(event.location)
    }

  /**
   * Creates a new delivery snapshot to reflect changes in routing, i.e.
   * when the route specification or the itinerary has changed
   * but no additional handling of the cargo has been performed.
   *
   * @param routeSpecification route specification
   * @param itinerary itinerary
   * @return An up to date delivery
   */
  def updateOnRouting(routeSpecification: RouteSpecification, itinerary: Itinerary): Delivery = {
    Validate.notNull(routeSpecification, "Route specification is required")
    new Delivery(lastEvent, itinerary, routeSpecification)
  }

  def sameValueAs(other: Delivery): Boolean =
    other != null && new EqualsBuilder()
      .append(transportStatus, other.transportStatus)
      .append(lastKnownLocation, other.lastKnownLocation)
      .append(currentVoyage, other.currentVoyage)
      .append(misdirected, other.misdirected)
      .append(eta, other.eta)
      .append(nextExpectedActivity, other.nextExpectedActivity)
      .append(unloadedAtDestination, other.unloadedAtDestination)
      .append(routingStatus, other.routingStatus)
      .append(calculatedAt, other.calculatedAt)
      .append(lastEvent, other.lastEvent)
      .isEquals()

  override def equals(other: Any): Boolean = other match {
    case other: Delivery => other.getClass == getClass && sameValueAs(other)
    case _               => false
  }
}

object Delivery {

  /**
   * Creates a new delivery snapshot based on the complete handling history of a cargo,
   * as well as its route specification and itinerary.
   *
   * @param routeSpecification route specification
   * @param itinerary itinerary
   * @param handlingHistory delivery history
   * @return An up to date delivery.
   */
  def derivedFrom(
      routeSpecification: RouteSpecification,
      itinerary: Itinerary,
      handlingHistory: HandlingHistory
  ): Delivery = {
    Validate.notNull(routeSpecification, "Route specification is required")
    Validate.notNull(handlingHistory, "Delivery history is required")

    val lastEvent = handlingHistory.mostRecentlyCompletedEvent
    new Delivery(lastEvent, itinerary, routeSpecification)
  }

}
