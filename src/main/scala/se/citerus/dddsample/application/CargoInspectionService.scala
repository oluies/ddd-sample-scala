package se.citerus.dddsample.application

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.shared.DomainError

/**
 * Cargo inspection service. Re-derives the cargo's delivery state from its
 * handling history and emits domain events for misdirection / arrival.
 *
 * Returns `Either[DomainError, Unit]` — `UnknownCargo` if the tracking id
 * isn't on file; `Right(())` on a successful inspection.
 */
trait CargoInspectionService:

  def inspectCargo(trackingId: TrackingId): Either[DomainError, Unit]
