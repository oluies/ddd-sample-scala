package se.citerus.dddsample.infrastructure.persistence.inmemory

import java.util.concurrent.CopyOnWriteArrayList

import scala.jdk.CollectionConverters.*

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.{
  HandlingEvent,
  HandlingEventRepository,
  HandlingHistory
}

/**
 * In-memory [[HandlingEventRepository]]. Stores events in an append-only
 * list (events are immutable value-like aggregates and are never updated).
 */
final class InMemoryHandlingEventRepository extends HandlingEventRepository:

  private val events = new CopyOnWriteArrayList[HandlingEvent]()

  override def store(event: HandlingEvent): Unit =
    events.add(event)

  override def lookupHandlingHistoryOfCargo(trackingId: TrackingId): HandlingHistory =
    val matching = events.asScala
      .filter(_.cargo.trackingId.sameValueAs(trackingId))
      .toList
    HandlingHistory(matching)
