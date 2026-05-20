package se.citerus.dddsample.domain.model.cargo

import java.util.Objects

import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * A predicted handling activity: where and how the cargo is next expected to
 * be handled. Lives in the `cargo` package upstream (used by `Delivery`),
 * not in `handling`.
 *
 * `voyage` is optional — `RECEIVE` / `CLAIM` activities don't have one. We
 * model that as `Option[Voyage]` rather than the upstream nullable field.
 */
final case class HandlingActivity(
    eventType: HandlingEventType,
    location: Location,
    voyage: Option[Voyage]
) extends ValueObject[HandlingActivity]:
  Objects.requireNonNull(eventType, "Handling event type is required")
  Objects.requireNonNull(location, "Location is required")
  Objects.requireNonNull(voyage, "voyage must not be null Option")

  override def sameValueAs(other: HandlingActivity): Boolean =
    other != null && this == other

object HandlingActivity:
  def apply(eventType: HandlingEventType, location: Location): HandlingActivity =
    HandlingActivity(eventType, location, None)

  def apply(eventType: HandlingEventType, location: Location, voyage: Voyage): HandlingActivity =
    Objects.requireNonNull(voyage, "Voyage is required")
    HandlingActivity(eventType, location, Some(voyage))
