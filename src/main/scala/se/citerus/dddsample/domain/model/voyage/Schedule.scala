package se.citerus.dddsample.domain.model.voyage

import java.util.Objects

import se.citerus.dddsample.domain.shared.ValueObject

/**
 * A voyage schedule — an ordered list of [[CarrierMovement]]s.
 *
 * Production schedules must contain at least one non-null movement; the
 * `apply` factory enforces this. The sentinel [[Schedule.EMPTY]] used only by
 * [[Voyage.NONE]] bypasses the check via the private constructor.
 */
final class Schedule private (val carrierMovements: List[CarrierMovement])
    extends ValueObject[Schedule]:

  override def sameValueAs(other: Schedule): Boolean =
    other != null && this.carrierMovements == other.carrierMovements

  override def equals(o: Any): Boolean = o match
    case that: Schedule => sameValueAs(that)
    case _              => false

  override def hashCode: Int = carrierMovements.hashCode

object Schedule:
  /**
   * Null-object schedule used only by [[Voyage.NONE]]. Not a valid schedule
   * for live cargo; production code uses [[Schedule.apply]] which enforces
   * non-emptiness.
   */
  val EMPTY: Schedule = new Schedule(Nil)

  def apply(carrierMovements: List[CarrierMovement]): Schedule =
    Objects.requireNonNull(carrierMovements, "carrierMovements must not be null")
    require(!carrierMovements.contains(null), "carrierMovements must not contain null")
    require(carrierMovements.nonEmpty, "carrierMovements must not be empty")
    new Schedule(carrierMovements)
