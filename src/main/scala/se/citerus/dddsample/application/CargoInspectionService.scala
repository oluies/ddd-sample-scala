package se.citerus.dddsample.application

;

import se.citerus.dddsample.domain.model.cargo.TrackingId;

/**
 * Cargo inspection service.
 */
trait CargoInspectionService {

  /**
   * Inspect cargo and send relevant notifications to interested parties,
   * for example if a cargo has been misdirected, or unloaded
   * at the final destination.
   *
   * @param trackingId cargo tracking id
   */
  def inspectCargo(trackingId: TrackingId): Unit

}
