package se.citerus.dddsample.domain.model.cargo

;

import se.citerus.dddsample.domain.shared.ValueObject;

/**
 * Represents the different transport statuses for a cargo.
 */
sealed abstract class TransportStatus extends ValueObject[TransportStatus] {
  def sameValueAs(other: TransportStatus): Boolean = {
    other != null && this.equals(other)
  }
}

case object NOT_RECEIVED extends TransportStatus
case object IN_PORT extends TransportStatus
case object ONBOARD_CARRIER extends TransportStatus
case object CLAIMED extends TransportStatus
case object UNKNOWN extends TransportStatus
