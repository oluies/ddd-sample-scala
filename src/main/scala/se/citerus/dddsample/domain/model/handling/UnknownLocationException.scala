package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.location.UnLocode

case class UnknownLocationException(val unlocode:UnLocode) extends Exception() {
  override def getMessage = "No location with UN locode " + unlocode.idString + " exists in the system"
}