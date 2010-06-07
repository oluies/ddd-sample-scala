package se.citerus.dddsample.domain.model.voyage

import se.citerus.dddsample.domain.shared.ValueObject
import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * A voyage schedule.
 *
 */
class Schedule(val carrierMovements: List[CarrierMovement] = List()) extends ValueObject[Schedule] {
  require(carrierMovements != null, "carrierMovements cannot be null")
  require(!carrierMovements.exists((n) => n == null), "carrierMovements cannot contain null elements")
  require(!carrierMovements.isEmpty, "carrierMovements cannot be empty")

  override def sameValueAs(other: Schedule): Boolean = {
    other != null && carrierMovements.equals(other.carrierMovements)
  }

  override def equals(other: Any): Boolean = other match {
    case other: Schedule => other.getClass == getClass && sameValueAs(other)
    case _ => false
  }

  override def hashCode: Int = {new HashCodeBuilder().append(this.carrierMovements).toHashCode()}
}

object Schedule {
  val EMPTY = new Schedule()
}