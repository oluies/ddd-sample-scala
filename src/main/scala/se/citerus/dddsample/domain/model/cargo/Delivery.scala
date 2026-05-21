package se.citerus.dddsample.domain.model.cargo

import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.handling.{
  HandlingEvent,
  HandlingEventType,
  HandlingHistory
}
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * The actual transportation state of a cargo, as opposed to the customer
 * requirement ([[RouteSpecification]]) and the plan ([[Itinerary]]).
 *
 * Pure value object — replaced wholesale whenever routing or handling
 * changes. Recomputes all derived state in the constructor.
 */
final class Delivery private (
    val lastEvent: Option[HandlingEvent],
    itineraryOpt: Option[Itinerary],
    routeSpecification: RouteSpecification,
    val calculatedAt: Instant
) extends ValueObject[Delivery]:

  val transportStatus: TransportStatus = Delivery.calculateTransportStatus(lastEvent)
  val routingStatus: RoutingStatus =
    Delivery.calculateRoutingStatus(itineraryOpt, routeSpecification)
  val misdirected: Boolean = Delivery.calculateMisdirected(lastEvent, itineraryOpt)
  val lastKnownLocationOpt: Option[Location] = lastEvent.map(_.location)
  val currentVoyage: Option[Voyage] =
    if transportStatus == TransportStatus.ONBOARD_CARRIER then lastEvent.flatMap(_.voyage)
    else None
  val eta: Option[Instant] =
    if isOnTrack then itineraryOpt.map(_.finalArrivalDate) else None
  val nextExpectedActivity: Option[HandlingActivity] =
    Delivery.calculateNextExpectedActivity(isOnTrack, lastEvent, routeSpecification, itineraryOpt)
  val isUnloadedAtDestination: Boolean =
    Delivery.calculateUnloadedAtDestination(lastEvent, routeSpecification)

  def isMisdirected: Boolean = misdirected

  /** Last known location, or [[Location.UNKNOWN]] if no events received. */
  def lastKnownLocation: Location = lastKnownLocationOpt.getOrElse(Location.UNKNOWN)

  private def isOnTrack: Boolean = routingStatus == RoutingStatus.ROUTED && !misdirected

  /**
   * Snapshot reflecting only a routing change — no new handling, but the
   * itinerary or specification has changed.
   */
  def updateOnRouting(
      spec: RouteSpecification,
      itinerary: Option[Itinerary],
      now: Instant = Instant.now()
  ): Delivery =
    Objects.requireNonNull(spec, "Route specification is required")
    new Delivery(lastEvent, itinerary, spec, now)

  override def sameValueAs(other: Delivery): Boolean =
    other != null &&
      transportStatus == other.transportStatus &&
      lastKnownLocationOpt == other.lastKnownLocationOpt &&
      currentVoyage == other.currentVoyage &&
      misdirected == other.misdirected &&
      eta == other.eta &&
      nextExpectedActivity == other.nextExpectedActivity &&
      isUnloadedAtDestination == other.isUnloadedAtDestination &&
      routingStatus == other.routingStatus &&
      calculatedAt == other.calculatedAt &&
      lastEvent == other.lastEvent

  override def equals(o: Any): Boolean = o match
    case that: Delivery => sameValueAs(that)
    case _              => false

  override def hashCode: Int =
    var h = transportStatus.hashCode
    h = 31 * h + lastKnownLocationOpt.hashCode
    h = 31 * h + currentVoyage.hashCode
    h = 31 * h + misdirected.hashCode
    h = 31 * h + eta.hashCode
    h = 31 * h + nextExpectedActivity.hashCode
    h = 31 * h + isUnloadedAtDestination.hashCode
    h = 31 * h + routingStatus.hashCode
    h = 31 * h + calculatedAt.hashCode
    h = 31 * h + lastEvent.hashCode
    h

object Delivery:

  /**
   * Builds a new snapshot from the cargo's current handling history.
   *
   * `now` defaults to `Instant.now()`; pass an explicit value (e.g. a
   * fixed test clock) when deterministic `calculatedAt` matters.
   */
  def derivedFrom(
      routeSpecification: RouteSpecification,
      itinerary: Option[Itinerary],
      handlingHistory: HandlingHistory,
      now: Instant = Instant.now()
  ): Delivery =
    Objects.requireNonNull(routeSpecification, "Route specification is required")
    Objects.requireNonNull(handlingHistory, "Delivery history is required")
    new Delivery(handlingHistory.mostRecentlyCompletedEvent, itinerary, routeSpecification, now)

  private def calculateTransportStatus(last: Option[HandlingEvent]): TransportStatus =
    last match
      case None => TransportStatus.NOT_RECEIVED
      case Some(ev) =>
        ev.eventType match
          case HandlingEventType.LOAD    => TransportStatus.ONBOARD_CARRIER
          case HandlingEventType.UNLOAD  => TransportStatus.IN_PORT
          case HandlingEventType.RECEIVE => TransportStatus.IN_PORT
          case HandlingEventType.CUSTOMS => TransportStatus.IN_PORT
          case HandlingEventType.CLAIM   => TransportStatus.CLAIMED

  private def calculateRoutingStatus(
      itinerary: Option[Itinerary],
      spec: RouteSpecification
  ): RoutingStatus =
    itinerary match
      case None => RoutingStatus.NOT_ROUTED
      case Some(i) =>
        if spec.isSatisfiedBy(i) then RoutingStatus.ROUTED else RoutingStatus.MISROUTED

  private def calculateMisdirected(
      last: Option[HandlingEvent],
      itinerary: Option[Itinerary]
  ): Boolean =
    (last, itinerary) match
      case (Some(ev), Some(i)) => !i.isExpected(ev)
      case _                   => false

  private def calculateUnloadedAtDestination(
      last: Option[HandlingEvent],
      spec: RouteSpecification
  ): Boolean =
    last.exists(ev =>
      ev.eventType == HandlingEventType.UNLOAD &&
        spec.destination.sameIdentityAs(ev.location)
    )

  private def calculateNextExpectedActivity(
      onTrack: Boolean,
      last: Option[HandlingEvent],
      spec: RouteSpecification,
      itinerary: Option[Itinerary]
  ): Option[HandlingActivity] =
    if !onTrack then None
    else
      last match
        case None => Some(HandlingActivity(HandlingEventType.RECEIVE, spec.origin))
        case Some(ev) =>
          val legs = itinerary.map(_.legs).getOrElse(Nil)
          ev.eventType match
            case HandlingEventType.LOAD =>
              legs
                .find(_.loadLocation.sameIdentityAs(ev.location))
                .map(l => HandlingActivity(HandlingEventType.UNLOAD, l.unloadLocation, l.voyage))
            case HandlingEventType.UNLOAD =>
              val idx = legs.indexWhere(_.unloadLocation.sameIdentityAs(ev.location))
              if idx < 0 then None
              else if idx == legs.length - 1 then
                Some(HandlingActivity(HandlingEventType.CLAIM, legs(idx).unloadLocation))
              else
                val nextLeg = legs(idx + 1)
                Some(HandlingActivity(HandlingEventType.LOAD, nextLeg.loadLocation, nextLeg.voyage))
            case HandlingEventType.RECEIVE =>
              legs.headOption.map(l =>
                HandlingActivity(HandlingEventType.LOAD, l.loadLocation, l.voyage)
              )
            case HandlingEventType.CLAIM | HandlingEventType.CUSTOMS => None
