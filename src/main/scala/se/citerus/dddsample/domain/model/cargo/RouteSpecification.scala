package se.citerus.dddsample.domain.model.cargo


import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;

import se.citerus.dddsample.domain.shared.AbstractSpecification;
import se.citerus.dddsample.domain.shared.ValueObject;

import java.util.Date;

/**
 * Route specification. Describes where a cargo origin and destination is,
 * and the arrival deadline.
 *
 */
class RouteSpecification(val origin: Location, val destination: Location, val arrivalDeadline: Date)
        extends AbstractSpecification[Itinerary] with ValueObject[RouteSpecification]
{
  Validate.notNull(origin, "Origin is required");
  Validate.notNull(destination, "Destination is required");
  Validate.notNull(arrivalDeadline, "Arrival deadline is required");
  Validate.isTrue(!origin.sameIdentityAs(destination), "Origin and destination can't be the same: " + origin);

  def isSatisfiedBy(itinerary: Itinerary): Boolean = {
    return itinerary != null &&
            origin.sameIdentityAs(itinerary.initialDepartureLocation) &&
            destination.sameIdentityAs(itinerary.finalArrivalLocation) &&
            arrivalDeadline.after(itinerary.finalArrivalDate);
  }

  def sameValueAs(other: RouteSpecification): Boolean = {
    other != null && new EqualsBuilder().
            append(this.origin, other.origin).
            append(this.destination, other.destination).
            append(this.arrivalDeadline, other.arrivalDeadline).
            isEquals();
  }
  
  override def equals(other:Any) : Boolean = other match {
    case other: RouteSpecification => other.getClass == getClass && sameValueAs(other)
    case _ => false
  }
}