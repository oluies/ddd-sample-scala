package se.citerus.dddsample.domain.model.voyage

import java.time.Instant
import java.util.Objects

import scala.collection.mutable

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.shared.DomainEntity

/**
 * A Voyage aggregate.
 *
 * Pure domain entity — no JPA annotations. Identity is by [[VoyageNumber]];
 * equality delegates to [[sameIdentityAs]].
 */
final class Voyage(val voyageNumber: VoyageNumber, val schedule: Schedule)
    extends DomainEntity[Voyage]:
  Objects.requireNonNull(voyageNumber, "Voyage number is required")
  Objects.requireNonNull(schedule, "Schedule is required")

  override def sameIdentityAs(other: Voyage): Boolean =
    other != null && this.voyageNumber.sameValueAs(other.voyageNumber)

  override def equals(o: Any): Boolean = o match
    case that: Voyage => sameIdentityAs(that)
    case _            => false

  override def hashCode: Int = voyageNumber.hashCode

  override def toString: String = s"Voyage ${voyageNumber.idString}"

object Voyage:

  /** Null object pattern. */
  val NONE: Voyage = new Voyage(VoyageNumber(""), Schedule.EMPTY)

  /**
   * Builder for incremental construction of a [[Voyage]] aggregate. Serves as
   * the aggregate factory.
   */
  final class Builder(voyageNumber: VoyageNumber, initialDepartureLocation: Location):
    Objects.requireNonNull(voyageNumber, "Voyage number is required")
    Objects.requireNonNull(initialDepartureLocation, "Departure location is required")

    private val movements: mutable.ArrayBuffer[CarrierMovement] =
      mutable.ArrayBuffer.empty
    private var departureLocation: Location = initialDepartureLocation

    def addMovement(
        arrivalLocation: Location,
        departureTime: Instant,
        arrivalTime: Instant
    ): Builder =
      movements += CarrierMovement(
        departureLocation,
        arrivalLocation,
        departureTime,
        arrivalTime
      )
      // Next departure location is the same as this arrival location.
      departureLocation = arrivalLocation
      this

    def build(): Voyage = new Voyage(voyageNumber, Schedule(movements.toList))
