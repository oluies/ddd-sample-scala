package se.citerus.dddsample.domain.model.cargo

import java.util.Date

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.ValueObject

/**
 * An itinerary consists of one or more legs.
 */
class Leg(
    val voyage: Voyage,
    val loadLocation: Location,
    val unloadLocation: Location,
    private val _loadTime: Date,
    private val _unloadTime: Date
) extends ValueObject[Leg] {
  require(voyage != null, "voyage cannot be null")
  require(loadLocation != null, "loadLocation cannot be null")
  require(unloadLocation != null, "unloadLocation cannot be null")
  require(_loadTime != null, "loadTime cannot be null")
  require(_unloadTime != null, "unloadTime cannot be null")

  def loadTime(): Date = new Date(_loadTime.getTime())

  def unloadTime(): Date = new Date(_unloadTime.getTime())

  override def sameValueAs(other: Leg): Boolean =
    other != null && new EqualsBuilder()
      .append(this.voyage, other.voyage)
      .append(this.loadLocation, other.loadLocation)
      .append(this.unloadLocation, other.unloadLocation)
      .append(this._loadTime, other._loadTime)
      .append(this._unloadTime, other._unloadTime)
      .isEquals()

  override def equals(other: Any): Boolean = other match {
    case other: Leg => other.getClass == getClass && sameValueAs(other)
    case _          => false
  }

  override def hashCode: Int =
    new HashCodeBuilder()
      .append(voyage)
      .append(loadLocation)
      .append(unloadLocation)
      .append(_loadTime)
      .append(_unloadTime)
      .toHashCode()

}
