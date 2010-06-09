package se.citerus.dddsample.interfaces.booking.facade.dto

import java.util.Date

class CargoRoutingDTO(
        val trackingId: String,
        val origin: String,
        val finalDestination: String,
        val arrivalDeadline: Date,
        val misrouted: Boolean) {
  var legs: List[LegDTO] = List()
  
  
  def addLeg(voyageNumber:String, from:String, to:String, loadTime:Date, unloadTime:Date) = {
    legs = legs ::: List(new LegDTO(voyageNumber, from, to, loadTime, unloadTime));
  }

}