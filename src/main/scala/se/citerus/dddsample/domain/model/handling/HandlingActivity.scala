package se.citerus.dddsample.domain.model.handling

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.voyage.Voyage;
import se.citerus.dddsample.domain.shared.ValueObject;

/**
 * A handling activity represents how and where a cargo can be handled,
 * and can be used to express predictions about what is expected to
 * happen to a cargo in the future.
 *
 */
case class HandlingActivity(eventType:HandlingEventType, location:Location, voyage:Option[Voyage] = None)
  extends ValueObject[HandlingActivity] 
{
  Validate.notNull(eventType, "Handling event type is required");
  Validate.notNull(location, "Location is required");
  
  def this(eventType:HandlingEventType, location:Location) = { this(eventType, location, None) }
  
  def sameValueAs(other:HandlingActivity) : Boolean = {
    equals(other)
  }
}