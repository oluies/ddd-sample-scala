package se.citerus.dddsample.domain.model.handling

import java.time.Instant
import java.util.Objects

import se.citerus.dddsample.domain.model.cargo.Cargo
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.DomainEvent

/** A HandlingEvent records that a cargo was handled — for instance, unloaded
  * from a carrier — at a given location and time. HandlingEvents are sent
  * from incident-logging applications some time after the event actually
  * happened.
  *
  * Aggregate root: every HandlingEvent is the root of its own (single-class)
  * aggregate. Domain layer stays JPA-annotation-free; the persistence model
  * lives in `infrastructure.persistence.jpa` (phase 9).
  *
  * `voyage` is optional — `RECEIVE`, `CLAIM`, `CUSTOMS` events have no
  * voyage; `LOAD` / `UNLOAD` require one. We expose `voyage` as a public
  * field returning `Voyage` (defaulting to `Voyage.NONE` for the non-voyage
  * case) to mirror the upstream Java accessor.
  */
final class HandlingEvent private (
    val cargo: Cargo,
    val completionTime: Instant,
    val registrationTime: Instant,
    val eventType: HandlingEventType,
    val location: Location,
    private val voyageOpt: Option[Voyage]
) extends DomainEvent[HandlingEvent]:

  def voyage: Voyage = voyageOpt.getOrElse(Voyage.NONE)

  override def sameEventAs(other: HandlingEvent): Boolean =
    other != null &&
      cargo == other.cargo &&
      voyageOpt == other.voyageOpt &&
      completionTime == other.completionTime &&
      location == other.location &&
      eventType == other.eventType

  override def equals(o: Any): Boolean = o match
    case that: HandlingEvent => sameEventAs(that)
    case _                   => false

  override def hashCode: Int =
    var h = cargo.hashCode
    h = 31 * h + voyageOpt.hashCode
    h = 31 * h + completionTime.hashCode
    h = 31 * h + location.hashCode
    h = 31 * h + eventType.hashCode
    h

  override def toString: String =
    val builder = new StringBuilder("\n--- Handling event ---\n")
      .append("Cargo: ").append(cargo.trackingId.idString).append('\n')
      .append("Type: ").append(eventType).append('\n')
      .append("Location: ").append(location.name).append('\n')
      .append("Completed on: ").append(completionTime).append('\n')
      .append("Registered on: ").append(registrationTime).append('\n')
    voyageOpt.foreach(v => builder.append("Voyage: ").append(v.voyageNumber.idString).append('\n'))
    builder.toString

object HandlingEvent:

  /** Constructor for a voyage-associated event (`LOAD` or `UNLOAD`). Throws
    * `IllegalArgumentException` if `eventType.prohibitsVoyage`.
    */
  def apply(
      cargo: Cargo,
      completionTime: Instant,
      registrationTime: Instant,
      eventType: HandlingEventType,
      location: Location,
      voyage: Voyage
  ): HandlingEvent =
    Objects.requireNonNull(cargo,            "Cargo is required")
    Objects.requireNonNull(completionTime,   "Completion time is required")
    Objects.requireNonNull(registrationTime, "Registration time is required")
    Objects.requireNonNull(eventType,        "Handling event type is required")
    Objects.requireNonNull(location,         "Location is required")
    Objects.requireNonNull(voyage,           "Voyage is required")
    require(!eventType.prohibitsVoyage,
      s"Voyage is not allowed with event type $eventType")
    new HandlingEvent(cargo, completionTime, registrationTime, eventType, location, Some(voyage))

  /** Constructor for a non-voyage event (`RECEIVE`, `CLAIM`, `CUSTOMS`).
    * Throws `IllegalArgumentException` if `eventType.requiresVoyage`.
    */
  def apply(
      cargo: Cargo,
      completionTime: Instant,
      registrationTime: Instant,
      eventType: HandlingEventType,
      location: Location
  ): HandlingEvent =
    Objects.requireNonNull(cargo,            "Cargo is required")
    Objects.requireNonNull(completionTime,   "Completion time is required")
    Objects.requireNonNull(registrationTime, "Registration time is required")
    Objects.requireNonNull(eventType,        "Handling event type is required")
    Objects.requireNonNull(location,         "Location is required")
    require(!eventType.requiresVoyage,
      s"Voyage is required for event type $eventType")
    new HandlingEvent(cargo, completionTime, registrationTime, eventType, location, None)
