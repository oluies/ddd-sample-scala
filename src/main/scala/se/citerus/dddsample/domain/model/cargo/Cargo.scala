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
 * **Immutable.** Mutating operations (`specifyNewRoute`, `assignToRoute`,
 * `deriveDeliveryProgress`) return a *new* `Cargo` rather than mutating in
 * place. Callers — the application services — must `cargoRepository.store`
 * the returned value to persist the change.
 *
 * Identity is by `trackingId`; `equals`/`hashCode` ignore the other fields
 * (canonical entity semantics). `origin` is fixed at the cargo's first
 * route specification and survives subsequent `specifyNewRoute` calls.
 *
 * Pure domain — no JPA annotations (D1). The persistence model lives in
 * `infrastructure.persistence.jpa` (phase 9b).
 */
final case class Cargo private (
    trackingId: TrackingId,
    origin: Location,
    routeSpecification: RouteSpecification,
    itineraryOpt: Option[Itinerary],
    delivery: Delivery
) extends DomainEntity[Cargo]:

  /** Specifies a new route. Recomputes delivery synchronously. */
  def specifyNewRoute(spec: RouteSpecification): Cargo =
    Objects.requireNonNull(spec, "Route specification is required")
    copy(
      routeSpecification = spec,
      delivery = delivery.updateOnRouting(spec, itineraryOpt)
    )

  /** Attaches a new itinerary. Recomputes delivery synchronously. */
  def assignToRoute(itinerary: Itinerary): Cargo =
    Objects.requireNonNull(itinerary, "Itinerary is required for assignment")
    copy(
      itineraryOpt = Some(itinerary),
      delivery = delivery.updateOnRouting(routeSpecification, Some(itinerary))
    )

  /** Recomputes delivery from the cargo's handling history. */
  def deriveDeliveryProgress(handlingHistory: HandlingHistory): Cargo =
    copy(delivery =
      Delivery.derivedFrom(
        routeSpecification,
        itineraryOpt,
        handlingHistory.filterOnCargo(trackingId)
      )
    )

  override def sameIdentityAs(other: Cargo): Boolean =
    other != null && trackingId.sameValueAs(other.trackingId)

  // Identity-based equality (canonical Entity semantics): ignore everything
  // but trackingId. Overrides the case-class structural equals.
  override def equals(o: Any): Boolean = o match
    case that: Cargo => sameIdentityAs(that)
    case _           => false

  override def hashCode: Int = trackingId.hashCode

  override def toString: String = trackingId.idString

object Cargo:

  /**
   * Smart constructor: a fresh, not-yet-routed cargo with an empty
   * handling history. Origin is fixed at the route spec's origin.
   */
  def apply(trackingId: TrackingId, spec: RouteSpecification): Cargo =
    Objects.requireNonNull(trackingId, "Tracking ID is required")
    Objects.requireNonNull(spec, "Route specification is required")
    new Cargo(
      trackingId = trackingId,
      origin = spec.origin,
      routeSpecification = spec,
      itineraryOpt = None,
      delivery = Delivery.derivedFrom(spec, None, HandlingHistory.EMPTY)
    )
