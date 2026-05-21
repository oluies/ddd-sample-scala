package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.cargo.TrackingId

/** Repository for [[HandlingEvent]] aggregates. */
trait HandlingEventRepository:

  /** Stores a (new) handling event. */
  def store(event: HandlingEvent): Unit

  /** Returns the handling history of the cargo with the given tracking id. */
  def lookupHandlingHistoryOfCargo(trackingId: TrackingId): HandlingHistory
