package se.citerus.dddsample.domain.model.cargo

import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.shared.{AbstractSpecification, ValueObject}

/** Route specification — origin, destination, and arrival deadline. Acts as a
  * [[se.citerus.dddsample.domain.shared.Specification]] over [[Itinerary]]:
  * an itinerary satisfies the specification iff it leaves from the origin,
  * arrives at the destination, and lands before the deadline.
  */
final case class RouteSpecification(
    origin: Location,
    destination: Location,
    arrivalDeadline: Instant
) extends AbstractSpecification[Itinerary]
    with ValueObject[RouteSpecification]:
  Objects.requireNonNull(origin,          "Origin is required")
  Objects.requireNonNull(destination,     "Destination is required")
  Objects.requireNonNull(arrivalDeadline, "Arrival deadline is required")
  require(!origin.sameIdentityAs(destination),
    s"Origin and destination can't be the same: $origin")

  override def isSatisfiedBy(itinerary: Itinerary): Boolean =
    itinerary != null &&
      origin.sameIdentityAs(itinerary.initialDepartureLocation) &&
      destination.sameIdentityAs(itinerary.finalArrivalLocation) &&
      arrivalDeadline.isAfter(itinerary.finalArrivalDate)

  override def sameValueAs(other: RouteSpecification): Boolean =
    other != null && this == other
