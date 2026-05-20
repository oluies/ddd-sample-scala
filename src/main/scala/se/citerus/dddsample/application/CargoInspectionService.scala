package se.citerus.dddsample.application

import se.citerus.dddsample.domain.model.cargo.TrackingId

/** Cargo inspection service. Re-derives the cargo's delivery state from its
  * handling history and emits domain events for misdirection / arrival.
  */
trait CargoInspectionService:

  def inspectCargo(trackingId: TrackingId): Unit
