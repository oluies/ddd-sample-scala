package se.citerus.dddsample.domain.model.cargo

import se.citerus.dddsample.domain.shared.ValueObject

/** Transport status of a cargo. */
enum TransportStatus extends ValueObject[TransportStatus]:
  case NOT_RECEIVED, IN_PORT, ONBOARD_CARRIER, CLAIMED, UNKNOWN

  override def sameValueAs(other: TransportStatus): Boolean = this == other
