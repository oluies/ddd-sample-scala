package se.citerus.dddsample.domain.model.voyage

import se.citerus.dddsample.domain.shared.ValueObject

class VoyageNumber(val number: String) extends ValueObject[VoyageNumber] {

  def idString = number

  override def sameValueAs(other: VoyageNumber): Boolean =
    other != null && number.equals(other.number)

  override def equals(other: Any): Boolean = other match {
    case other: VoyageNumber => other.getClass == getClass && sameValueAs(other)
    case _                   => false
  }

  override def hashCode: Int = number.hashCode()

  override def toString: String = number
}
