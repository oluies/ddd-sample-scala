package se.citerus.dddsample.domain.model.voyage

import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * A carrier movement is a vessel voyage from one location to another.
 *
 * Pure domain value object — no JPA annotations. `case class` gives us
 * structural equality and `hashCode` for free; `require` enforces the
 * upstream Java `Validate.noNullElements` invariant.
 */
final case class CarrierMovement(
    departureLocation: Location,
    arrivalLocation: Location,
    departureTime: Instant,
    arrivalTime: Instant
) extends ValueObject[CarrierMovement]:
  Objects.requireNonNull(departureLocation, "departureLocation must not be null")
  Objects.requireNonNull(arrivalLocation, "arrivalLocation must not be null")
  Objects.requireNonNull(departureTime, "departureTime must not be null")
  Objects.requireNonNull(arrivalTime, "arrivalTime must not be null")

  override def sameValueAs(other: CarrierMovement): Boolean =
    other != null && this == other

object CarrierMovement:
  /** Null object pattern. */
  val NONE: CarrierMovement = CarrierMovement(
    Location.UNKNOWN,
    Location.UNKNOWN,
    Instant.ofEpochMilli(0),
    Instant.ofEpochMilli(0)
  )
