package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.cargo.TrackingId;

case class UnknownCargoException(val trackingId:TrackingId) extends Exception() {
  override def getMessage = "No cargo with tracking id " + trackingId.idString + " exists in the system"
}