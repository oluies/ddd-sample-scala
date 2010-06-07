package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.cargo.TrackingId;

/**
 * Handling event repository.
 */
trait HandlingEventRepository {

  /**
   * Stores a (new) handling event.
   *
   * @param event handling event to save
   */
  def store(event: HandlingEvent): Unit;

  /**
   * @param trackingId cargo tracking id
   * @return The handling history of this cargo
   */
  def lookupHandlingHistoryOfCargo(trackingId: TrackingId): HandlingHistory;
}