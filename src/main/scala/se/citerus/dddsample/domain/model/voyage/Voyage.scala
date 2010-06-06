package se.citerus.dddsample.domain.model.voyage

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.shared.Entity;
import java.util.Date;

object Voyage {	
  // Null object pattern
	val NONE : Voyage = new Voyage(new VoyageNumber(""), Schedule.EMPTY);
}

class Voyage(val voyageNumber:VoyageNumber, val schedule:Schedule) extends Entity[Voyage] {
  Validate.notNull(voyageNumber, "Voyage number is required");
  Validate.notNull(schedule, "Schedule is required");

  override def equals(other:Any) : Boolean = other match {
    case other: Voyage => other.getClass == getClass && sameIdentityAs(other)
    case _ => false 
  }
  
  override def hashCode : Int = { voyageNumber.hashCode() }
    
  override def sameIdentityAs(other:Voyage) : Boolean = {
    other != null && voyageNumber.sameValueAs(other.voyageNumber);
  }
  
  override def toString():String = { "Voyage " + voyageNumber }
}

/**
 * Builder pattern is used for incremental construction
 * of a Voyage aggregate. This serves as an aggregate factory. 
 */
class Builder(val voyageNumber:VoyageNumber, var departureLocation:Location, val carrierMovements : List[CarrierMovement] = List()) {
  
  Validate.notNull(voyageNumber, "Voyage number is required");
  Validate.notNull(departureLocation, "Departure location is required");

  def addMovement(arrivalLocation:Location, departureTime:Date, arrivalTime:Date) : Builder = {
    val newMovements = carrierMovements ::: List(new CarrierMovement(departureLocation, arrivalLocation, 
                        departureTime, arrivalTime)) 
    // Next departure location is the same as this arrival location
    val builder = new Builder(voyageNumber, arrivalLocation, newMovements)    
    builder
  }
  
  def build() : Voyage = { new Voyage(voyageNumber, new Schedule(carrierMovements)); } 
}