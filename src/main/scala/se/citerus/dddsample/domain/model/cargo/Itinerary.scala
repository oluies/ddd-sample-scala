package se.citerus.dddsample.domain.model.cargo

import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.handling.{HandlingEvent, HandlingEventType}
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * An itinerary — an ordered list of [[Leg]]s describing the route a cargo
 * follows from origin to destination.
 *
 * The empty `Itinerary` exists only as the [[Itinerary.EMPTY]] sentinel used
 * by [[Cargo]] when no route is yet assigned. Production itineraries created
 * via `apply` must be non-empty and contain no nulls.
 */
final class Itinerary private (val legs: List[Leg]) extends ValueObject[Itinerary]:

  /**
   * True if `event` is consistent with this itinerary. CUSTOMS events are
   * always accepted; the empty itinerary accepts every event.
   */
  def isExpected(event: HandlingEvent): Boolean =
    if legs.isEmpty then true
    else
      event.eventType match
        case HandlingEventType.RECEIVE =>
          legs.head.loadLocation == event.location
        case HandlingEventType.LOAD =>
          legs.exists(l =>
            l.loadLocation.sameIdentityAs(event.location) &&
              event.voyage.exists(v => l.voyage.sameIdentityAs(v))
          )
        case HandlingEventType.UNLOAD =>
          legs.exists(l => l.unloadLocation == event.location && event.voyage.contains(l.voyage))
        case HandlingEventType.CLAIM =>
          lastLeg.exists(_.unloadLocation == event.location)
        case HandlingEventType.CUSTOMS => true

  def initialDepartureLocation: Location =
    legs.headOption.map(_.loadLocation).getOrElse(Location.UNKNOWN)

  def finalArrivalLocation: Location =
    lastLeg.map(_.unloadLocation).getOrElse(Location.UNKNOWN)

  def finalArrivalDate: Instant =
    lastLeg.map(_.unloadTime).getOrElse(Instant.MAX)

  def lastLeg: Option[Leg] = legs.lastOption

  override def sameValueAs(other: Itinerary): Boolean =
    other != null && this.legs == other.legs

  override def equals(o: Any): Boolean = o match
    case that: Itinerary => sameValueAs(that)
    case _               => false

  override def hashCode: Int = legs.hashCode

object Itinerary:

  /**
   * Sentinel returned by `Cargo.itinerary` when no route has been assigned.
   * Not a valid production itinerary; bypasses the non-empty check via the
   * private constructor.
   */
  val EMPTY: Itinerary = new Itinerary(Nil)

  def apply(legs: List[Leg]): Itinerary =
    Objects.requireNonNull(legs, "legs must not be null")
    require(!legs.contains(null), "legs must not contain null")
    require(legs.nonEmpty, "legs must not be empty")
    new Itinerary(legs)
