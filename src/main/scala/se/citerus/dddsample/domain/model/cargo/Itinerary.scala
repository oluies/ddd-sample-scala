package se.citerus.dddsample.domain.model.cargo

import se.citerus.dddsample.domain.shared.ValueObject
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingEventType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.shared.Entity;
import java.util.Date;


object Itinerary {
  val EMPTY_ITINERARY: Itinerary = new Itinerary();
  val END_OF_DAYS: Date = new Date(Math.MAX_LONG);
  
  def apply(legs: List[Leg]) = {
    require(! legs.isEmpty, "legs cannot be empty")
    require(! legs.exists(_ == null), "no null elements allowed in list");
    
    new Itinerary(legs)
  }
}

/**
 * An itinerary.
 *
 */
class Itinerary private (val legs: List[Leg] = List()) extends ValueObject[Itinerary] {

  /**
   * Test if the given handling event is expected when executing this itinerary.
   *
   * @param event Event to test.
   * @return <code>true</code> if the event is expected
   */
  def isExpected(event: HandlingEvent): Boolean = {
    import se.citerus.dddsample.domain.model.handling._
    if (legs.isEmpty) {
      return true;
    }

    event.eventType match {
      case RECEIVE => {
        //Check that the first leg's origin is the event's location
        val leg: Leg = legs(0);
        return (leg.loadLocation.equals(event.location));
      }
      case LOAD => {
        //Check that the there is one leg with same load location and voyage
        legs.foreach {
          leg =>
            if (leg.loadLocation.sameIdentityAs(event.location) &&
                    leg.voyage.sameIdentityAs(event.voyage)) {
              return true;
            }
        }
        return false;
      }
      case UNLOAD => {
        //Check that the there is one leg with same unload location and voyage
        legs.foreach {
          leg =>
            if (leg.unloadLocation.equals(event.location) &&
                    leg.voyage.equals(event.voyage)) {
              return true;
            }
        }
        return false;
      }
      case CLAIM => {
        //Check that the last leg's destination is from the event's location
        val leg: Leg = lastLeg;
        return (leg.unloadLocation.equals(event.location));
      }
      case CUSTOMS => {
        return true
      }
    }
  }

  /**
   * @return The initial departure location.
   */
  def initialDepartureLocation(): Location = {
    if (legs.isEmpty) {
      return Location.UNKNOWN;
    } else {
      return legs(0).loadLocation;
    }
  }

  def finalArrivalLocation(): Location = {
    if (legs.isEmpty) {
      return Location.UNKNOWN;
    } else {
      return lastLeg.unloadLocation;
    }
  }

  /**
   * @return Date when cargo arrives at final destination.
   */
  def finalArrivalDate(): Date = {
    if (lastLeg == null) {
      return new Date(Itinerary.END_OF_DAYS.getTime());
    } else {
      return new Date(lastLeg.unloadTime().getTime());
    }
  }

  /**
   * @return The last leg on the itinerary.
   */
  def lastLeg: Leg = {
    if (legs.isEmpty) {
      return null;
    } else {
      return legs(legs.size - 1);
    }
  }

  /**
   * @param other itinerary to compare
   * @return <code>true</code> if the legs in this and the other itinerary are all equal.
   */
  override def sameValueAs(other: Itinerary): Boolean = {
    other != null && legs.equals(other.legs)
  }

  /**
   * @param other itinerary to compare
   * @return <code>true</code> if the legs in this and the other itinerary are all equal.
   */
  override def equals(other: Any): Boolean = other match {
    case other: Itinerary => other.getClass == getClass && sameValueAs(other)
    case _ => false
  }

  override def hashCode: Int = {legs.hashCode}
}