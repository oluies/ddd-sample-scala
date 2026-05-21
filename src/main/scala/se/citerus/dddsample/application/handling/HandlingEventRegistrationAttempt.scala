package se.citerus.dddsample.application.handling

import java.io.Serializable
import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber

/**
 * Transfer object for incoming handling-event registration attempts. Carries
 * the data needed to construct a [[se.citerus.dddsample.domain.model.handling.HandlingEvent]]
 * once the referenced cargo / voyage / location have been resolved.
 *
 * Lives in the `application.handling` package because it is the contract
 * between `ApplicationEvents` (application tier) and the report parser /
 * JMS consumers — the *application* layer owns it, not the interfaces
 * layer. The upstream Java project puts it in `interfaces.handling`,
 * which is a layer violation we don't carry forward.
 */
final case class HandlingEventRegistrationAttempt(
    registrationTime: Instant,
    completionTime: Instant,
    trackingId: TrackingId,
    voyageNumber: Option[VoyageNumber],
    eventType: HandlingEventType,
    unLocode: UnLocode
) extends Serializable:
  Objects.requireNonNull(registrationTime, "registrationTime is required")
  Objects.requireNonNull(completionTime, "completionTime is required")
  Objects.requireNonNull(trackingId, "trackingId is required")
  Objects.requireNonNull(
    voyageNumber,
    "voyageNumber must not be null Option (use None for non-voyage events)"
  )
  Objects.requireNonNull(eventType, "eventType is required")
  Objects.requireNonNull(unLocode, "unLocode is required")
