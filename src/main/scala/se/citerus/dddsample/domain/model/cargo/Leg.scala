package se.citerus.dddsample.domain.model.cargo

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.ValueObject
import java.util.Date
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * An itinerary consists of one or more legs.
 */
class Leg(val voyage:Voyage, val loadLocation:Location, val unloadLocation:Location, loadTime:Date, unloadTime:Date) extends ValueObject[Leg] {

  require(voyage != null, "voyage cannot be null") 
  require(loadLocation != null, "loadLocation cannot be null")
  require(unloadLocation != null, "unloadLocation cannot be null") 
  require(loadTime != null, "loadTime cannot be null") 
  require(unloadTime != null, "unloadTime cannot be null") 
  
  def loadTime():Date = { new Date(loadTime.getTime()) }
  def unloadTime():Date = { new Date(unloadTime.getTime()) }

  override def sameValueAs(other:Leg) : Boolean = {
    other != null && new EqualsBuilder().
      append(this.voyage, other.voyage).
      append(this.loadLocation, other.loadLocation).
      append(this.unloadLocation, other.unloadLocation).
      append(this.loadTime, other.loadTime).
      append(this.unloadTime, other.unloadTime).
      isEquals();
  }
		
  override def equals(other:Any) : Boolean = other match {
    case other: Leg => other.getClass == getClass && sameValueAs(other)
    case _ => false 
  }
  
  override def hashCode : Int = {
    new HashCodeBuilder().
      append(voyage).
      append(loadLocation).
      append(unloadLocation).
      append(loadTime).
      append(unloadTime).
      toHashCode();
  }

}