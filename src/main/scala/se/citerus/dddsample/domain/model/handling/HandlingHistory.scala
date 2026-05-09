package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.shared.ValueObject

/**
 * The handling history of a cargo.
 */
class HandlingHistory(val handlingEvents: List[HandlingEvent])
    extends ValueObject[HandlingHistory] {
  def sameValueAs(other: HandlingHistory): Boolean =
    other != null && handlingEvents.equals(other.handlingEvents)

  def distinctEventsByCompletionTime: List[HandlingEvent] =
    handlingEvents.sortWith((s, t) => s.completionTime.compareTo(t.completionTime) < 0)

  def mostRecentlyCompletedEvent: Option[HandlingEvent] = {
    val distinctEvents = distinctEventsByCompletionTime
    if (distinctEvents.isEmpty) {
      None
    } else {
      Some(distinctEvents(distinctEvents.size - 1))
    }
  }
}

object HandlingHistory {
  val EMPTY = new HandlingHistory(List())
}
