package se.citerus.dddsample.interfaces.handling

import java.io.Serializable
import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber

/** Transfer object for incoming handling-event registration attempts. Used as
  * a JMS message payload — implements [[Serializable]].
  *
  * Lives in `interfaces.handling` upstream but is referenced by
  * `application.ApplicationEvents`, so it lands now (phase 8) and the rest
  * of the interfaces.handling package will follow in phase 14.
  */
final case class HandlingEventRegistrationAttempt(
    registrationTime: Instant,
    completionTime: Instant,
    trackingId: TrackingId,
    voyageNumber: VoyageNumber,
    eventType: HandlingEventType,
    unLocode: UnLocode
) extends Serializable:
  Objects.requireNonNull(registrationTime, "registrationTime is required")
  Objects.requireNonNull(completionTime,   "completionTime is required")
  Objects.requireNonNull(trackingId,       "trackingId is required")
  Objects.requireNonNull(voyageNumber,     "voyageNumber is required")
  Objects.requireNonNull(eventType,        "eventType is required")
  Objects.requireNonNull(unLocode,         "unLocode is required")
