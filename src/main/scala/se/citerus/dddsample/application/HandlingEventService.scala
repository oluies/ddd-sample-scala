package se.citerus.dddsample.application

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber

/** Handling event service. */
trait HandlingEventService:

  /** Registers a handling event in the system and notifies interested parties
    * that a cargo has been handled.
    *
    * Throws
    * [[se.citerus.dddsample.domain.model.handling.CannotCreateHandlingEventException]]
    * if the parameters don't represent a valid event we can track.
    */
  def registerHandlingEvent(
      completionTime: Instant,
      trackingId: TrackingId,
      voyageNumber: Option[VoyageNumber],
      unLocode: UnLocode,
      eventType: HandlingEventType
  ): Unit
