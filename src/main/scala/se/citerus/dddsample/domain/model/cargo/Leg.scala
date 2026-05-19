package se.citerus.dddsample.domain.model.cargo

import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.ValueObject

/** A single leg of an itinerary — a voyage segment from a load location at a
  * load time to an unload location at an unload time.
  */
final case class Leg(
    voyage: Voyage,
    loadLocation: Location,
    unloadLocation: Location,
    loadTime: Instant,
    unloadTime: Instant
) extends ValueObject[Leg]:
  Objects.requireNonNull(voyage,         "voyage is required")
  Objects.requireNonNull(loadLocation,   "loadLocation is required")
  Objects.requireNonNull(unloadLocation, "unloadLocation is required")
  Objects.requireNonNull(loadTime,       "loadTime is required")
  Objects.requireNonNull(unloadTime,     "unloadTime is required")

  override def sameValueAs(other: Leg): Boolean = other != null && this == other
