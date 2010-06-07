package se.citerus.dddsample.interfaces.booking.facade.dto

import java.util.Date

class CargoRoutingDTO(
        val trackingId: String,
        val origin: String,
        val finalDestination: String,
        val arrivalDeadline: Date,
        val misrouted: Boolean) {
  val legs: List[LegDTO] = List()
}