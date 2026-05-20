package se.citerus.dddsample.domain.model.cargo

import java.util.Objects

import se.citerus.dddsample.domain.model.handling.HandlingHistory
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.shared.DomainEntity

/**
 * The Cargo aggregate root. Identifies a particular cargo by [[TrackingId]]
 * and ties together its [[RouteSpecification]], [[Itinerary]], and
 * [[Delivery]] snapshot.
 *
 * Behaviour mirrors the upstream Java aggregate:
 *
 *   - `specifyNewRoute` updates the route spec and recomputes delivery
 *     synchronously.
 *   - `assignToRoute` attaches a new itinerary and recomputes delivery
 *     synchronously.
 *   - `deriveDeliveryProgress` accepts a handling history and recomputes the
 *     delivery snapshot asynchronously (handling events live in a separate
 *     aggregate).
 *
 * Pure domain — no JPA annotations. The persistence model lives in
 * `infrastructure.persistence.jpa` (phase 9).
 */
final class Cargo(val trackingId: TrackingId, initialSpec: RouteSpecification)
    extends DomainEntity[Cargo]:
  Objects.requireNonNull(trackingId, "Tracking ID is required")
  Objects.requireNonNull(initialSpec, "Route specification is required")

  /** Origin never changes, even when the spec changes. */
  val origin: Location = initialSpec.origin

  private var _routeSpecification: RouteSpecification = initialSpec
  private var _itinerary: Option[Itinerary]           = None
  private var _delivery: Delivery =
    Delivery.derivedFrom(_routeSpecification, None, HandlingHistory.EMPTY)

  /** Secondary constructor for a cargo created already routed. */
  def this(trackingId: TrackingId, spec: RouteSpecification, itinerary: Itinerary) =
    this(trackingId, spec)
    Objects.requireNonNull(itinerary, "Itinerary is required")
    _itinerary = Some(itinerary)
    _delivery = Delivery.derivedFrom(_routeSpecification, _itinerary, HandlingHistory.EMPTY)

  def routeSpecification: RouteSpecification = _routeSpecification
  def itinerary: Itinerary                   = _itinerary.getOrElse(Itinerary.EMPTY)
  def delivery: Delivery                     = _delivery

  /** Specifies a new route. Recomputes delivery synchronously. */
  def specifyNewRoute(spec: RouteSpecification): Unit =
    Objects.requireNonNull(spec, "Route specification is required")
    _routeSpecification = spec
    _delivery = _delivery.updateOnRouting(spec, _itinerary)

  /** Attaches a new itinerary. Recomputes delivery synchronously. */
  def assignToRoute(itinerary: Itinerary): Unit =
    Objects.requireNonNull(itinerary, "Itinerary is required for assignment")
    _itinerary = Some(itinerary)
    _delivery = _delivery.updateOnRouting(_routeSpecification, _itinerary)

  /** Recomputes delivery from the cargo's handling history. */
  def deriveDeliveryProgress(handlingHistory: HandlingHistory): Unit =
    _delivery = Delivery.derivedFrom(
      _routeSpecification,
      _itinerary,
      handlingHistory.filterOnCargo(trackingId)
    )

  override def sameIdentityAs(other: Cargo): Boolean =
    other != null && trackingId.sameValueAs(other.trackingId)

  override def equals(o: Any): Boolean = o match
    case that: Cargo => sameIdentityAs(that)
    case _           => false

  override def hashCode: Int = trackingId.hashCode

  override def toString: String = trackingId.idString
