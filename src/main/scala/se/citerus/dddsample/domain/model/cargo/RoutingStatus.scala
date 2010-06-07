package se.citerus.dddsample.domain.model.cargo

;

import se.citerus.dddsample.domain.shared.ValueObject;

/**
 * Routing status. 
 */
sealed abstract class RoutingStatus extends ValueObject[RoutingStatus] {
  def sameValueAs(other: RoutingStatus): Boolean = {
    other != null && this.equals(other)
  }
}

case object NOT_ROUTED extends RoutingStatus
case object ROUTED extends RoutingStatus
case object MISROUTED extends RoutingStatus
