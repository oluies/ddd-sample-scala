package se.citerus.dddsample.interfaces.booking.facade.dto

import java.io.Serializable
import java.time.Instant

/** DTO presenting a cargo's current routing state to a booking clerk. */
final case class CargoRoutingDTO(
    trackingId: String,
    origin: String,
    finalDestination: String,
    arrivalDeadline: Instant,
    misrouted: Boolean,
    legs: List[LegDTO]
) extends Serializable:
  def routed: Boolean = legs.nonEmpty
