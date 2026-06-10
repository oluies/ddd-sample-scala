package se.citerus.dddsample.application

import java.time.Instant

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber
import se.citerus.dddsample.domain.shared.DomainError

/** Handling event service. */
trait HandlingEventService:

  /**
   * Registers a handling event in the system and notifies interested parties
   * that a cargo has been handled.
   *
   * Returns `Either[DomainError, Unit]` — `UnknownCargo` / `UnknownLocation`
   * / `UnknownVoyage` for failed lookups, `InvariantViolation` for events
   * that contradict the handling-event invariants (e.g. LOAD without voyage).
   */
  def registerHandlingEvent(
      completionTime: Instant,
      trackingId: TrackingId,
      voyageNumber: Option[VoyageNumber],
      unLocode: UnLocode,
      eventType: HandlingEventType
  ): Either[DomainError, Unit]
