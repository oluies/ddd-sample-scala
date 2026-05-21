package se.citerus.dddsample.domain.model.voyage

import java.util.Objects

import se.citerus.dddsample.domain.shared.ValueObject

/**
 * A voyage schedule — an ordered list of [[CarrierMovement]]s.
 *
 * Always non-empty: the `apply` factory rejects null, empty, or
 * null-containing input. The private constructor is sealed so no caller
 * can sneak past the invariant.
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

  def apply(carrierMovements: List[CarrierMovement]): Schedule =
    Objects.requireNonNull(carrierMovements, "carrierMovements must not be null")
    require(!carrierMovements.contains(null), "carrierMovements must not contain null")
    require(carrierMovements.nonEmpty, "carrierMovements must not be empty")
    new Schedule(carrierMovements)
