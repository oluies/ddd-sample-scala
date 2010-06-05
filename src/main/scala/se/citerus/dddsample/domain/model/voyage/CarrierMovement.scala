package se.citerus.dddsample.domain.model.voyage

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.shared.ValueObject;

import java.util.Date;
/**
 * A carrier movement is a vessel voyage from one location to another.
 * 
 * @param departureLocation location of departure
 * @param arrivalLocation location of arrival
 * @param departureTime time of departure
 * @param arrivalTime time of arrival
 */
class CarrierMovement(val departureLocation:Location, val arrivalLocation:Location, departureTime:Date, arrivalTime:Date) {

  Validate.noNullElements(Array(departureLocation, arrivalLocation, departureTime, arrivalTime))
  
  def departureTime() : Date = new Date(departureTime.getTime())
  def arrivalTime() : Date = new Date(arrivalTime.getTime())
  
//  @Override
//  public boolean equals(final Object o) {
//    if (this == o) return true;
//    if (o == null || getClass() != o.getClass()) return false;
//
//    final CarrierMovement that = (CarrierMovement) o;
//
//    return sameValueAs(that);
//  }
//
//  @Override
//  public int hashCode() {
//    return new HashCodeBuilder().
//      append(this.departureLocation).
//      append(this.departureTime).
//      append(this.arrivalLocation).
//      append(this.arrivalTime).
//      toHashCode();
//  }
//
//  @Override
//  public boolean sameValueAs(CarrierMovement other) {
//    return other != null && new EqualsBuilder().
//      append(this.departureLocation, other.departureLocation).
//      append(this.departureTime, other.departureTime).
//      append(this.arrivalLocation, other.arrivalLocation).
//      append(this.arrivalTime, other.arrivalTime).
//      isEquals();
//  }
//
//  CarrierMovement() {
//    // Needed by Hibernate
//  }
//
//  // Auto-generated surrogate key
//  private Long id;
}

object CarrierMovement {
  // Null object pattern 
  val NONE:CarrierMovement = new CarrierMovement(
    Location.UNKNOWN, Location.UNKNOWN,
    new Date(0), new Date(0)
  );
  
}