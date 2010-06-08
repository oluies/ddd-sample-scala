package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.voyage.VoyageNumber

class UnknownVoyageException(val voyageNumber:VoyageNumber) extends Exception() {
  override def getMessage = "No voyage with number " + voyageNumber.idString + " exists in the system"
}