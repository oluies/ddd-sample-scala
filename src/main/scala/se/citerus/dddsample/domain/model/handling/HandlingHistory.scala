package se.citerus.dddsample.domain.model.handling

import java.util.Objects

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * The handling history of a cargo — a (possibly unordered, possibly
 * duplicate-laden) collection of [[HandlingEvent]]s.
 */
final class HandlingHistory private (private val events: List[HandlingEvent])
    extends ValueObject[HandlingHistory]:

  /**
   * Distinct events ordered by completion time. Deduplication uses
   * `equals`/`hashCode` on `HandlingEvent`.
   */
  def distinctEventsByCompletionTime: List[HandlingEvent] =
    events.distinct.sortBy(_.completionTime)

  /**
   * Most recently completed event, or `None` if empty. The Java upstream
   * returns nullable `HandlingEvent`; Scala uses `Option`.
   */
  def mostRecentlyCompletedEvent: Option[HandlingEvent] =
    distinctEventsByCompletionTime.lastOption

  /**
   * Returns a new history containing only events whose cargo's tracking id
   * matches `trackingId`.
   */
  def filterOnCargo(trackingId: TrackingId): HandlingHistory =
    new HandlingHistory(events.filter(e => e.cargo.trackingId.sameValueAs(trackingId)))

  override def sameValueAs(other: HandlingHistory): Boolean =
    other != null && this.events == other.events

  override def equals(o: Any): Boolean = o match
    case that: HandlingHistory => sameValueAs(that)
    case _                     => false

  override def hashCode: Int = events.hashCode

object HandlingHistory:

  val EMPTY: HandlingHistory = new HandlingHistory(Nil)

  def apply(events: Iterable[HandlingEvent]): HandlingHistory =
    Objects.requireNonNull(events, "Handling events are required")
    new HandlingHistory(events.toList)
