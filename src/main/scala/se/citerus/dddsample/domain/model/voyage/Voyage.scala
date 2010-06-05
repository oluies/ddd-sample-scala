package se.citerus.dddsample.domain.model.voyage

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.shared.Entity;
import java.util.Date;

class Voyage(val voyageNumber:VoyageNumber, val schedule:Schedule) extends Entity[Voyage] {
//
//  private VoyageNumber voyageNumber;
//  private Schedule schedule;
//
//  // Null object pattern
//  public static final Voyage NONE = new Voyage(
//    new VoyageNumber(""), Schedule.EMPTY
//  );
//
//  public Voyage(final VoyageNumber voyageNumber, final Schedule schedule) {
//    Validate.notNull(voyageNumber, "Voyage number is required");
//    Validate.notNull(schedule, "Schedule is required");
//
//    this.voyageNumber = voyageNumber;
//    this.schedule = schedule;
//  }
//
//  /**
//   * @return Voyage number.
//   */
//  public VoyageNumber voyageNumber() {
//    return voyageNumber;
//  }
//
//  /**
//   * @return Schedule.
//   */
//  public Schedule schedule() {
//    return schedule;
//  }
//
//  @Override
//  public int hashCode() {
//    return voyageNumber.hashCode();
//  }
//
//  @Override
//  public boolean equals(Object o) {
//    if (this == o) return true;
//    if (o == null) return false;
//    if (!(o instanceof Voyage)) return false;
//
//    final Voyage that = (Voyage) o;
//
//    return sameIdentityAs(that);
//  }
//
//  @Override
//  public boolean sameIdentityAs(Voyage other) {
//    return other != null && this.voyageNumber().sameValueAs(other.voyageNumber());
//  }

  override def sameIdentityAs(other:Voyage) : Boolean = {
    other != null && voyageNumber.sameValueAs(other.voyageNumber);
  }
  
//  @Override
//  public String toString() {
//    return "Voyage " + voyageNumber;
//  }
//
//  Voyage() {
//    // Needed by Hibernate
//  }
//
//  // Needed by Hibernate
//  private Long id;
}

/**
 * Builder pattern is used for incremental construction
 * of a Voyage aggregate. This serves as an aggregate factory. 
 */
class Builder(val voyageNumber:VoyageNumber, var departureLocation:Location) {

  private var carrierMovements : List[CarrierMovement] = List()
  //private var departureLocation = departureLocation
  
  Validate.notNull(voyageNumber, "Voyage number is required");
  Validate.notNull(departureLocation, "Departure location is required");

  def addMovement(arrivalLocation:Location, departureTime:Date, arrivalTime:Date) : Builder = {
     carrierMovements = new CarrierMovement(departureLocation, arrivalLocation, 
    		 								departureTime, arrivalTime) :: carrierMovements 
      // Next departure location is the same as this arrival location
      this.departureLocation = arrivalLocation;
      this
  }
  
  def build() : Voyage = { new Voyage(voyageNumber, new Schedule(carrierMovements)); } 
}