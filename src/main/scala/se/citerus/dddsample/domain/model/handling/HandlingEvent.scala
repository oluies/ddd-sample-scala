package se.citerus.dddsample.domain.model.handling

import java.util.Date

import org.apache.commons.lang3.Validate
import org.apache.commons.lang3.builder.EqualsBuilder

import se.citerus.dddsample.domain.model.cargo.Cargo
import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.domain.model.voyage.Voyage
import se.citerus.dddsample.domain.shared.DomainEvent

/**
 * A HandlingEvent is used to register the event when, for instance,
 * a cargo is unloaded from a carrier at a some loacation at a given time.
 */
class HandlingEvent(
    val cargo: Cargo,
    val completionTime: Date,
    val registrationTime: Date,
    val eventType: HandlingEventType,
    val location: Location,
    val voyage: Voyage
) extends DomainEvent[HandlingEvent] {
  Validate.notNull(cargo, "Cargo is required")
  Validate.notNull(completionTime, "Completion time is required")
  Validate.notNull(registrationTime, "Registration time is required")
  Validate.notNull(eventType, "Handling event type is required")
  Validate.notNull(location, "Location is required")

  require(!eventType.prohibitsVoyage(), "Voyage is not allowed with event type " + eventType)

  def sameEventAs(other: HandlingEvent): Boolean =
    other != null && new EqualsBuilder()
      .append(this.cargo, other.cargo)
      .append(this.voyage, other.voyage)
      .append(this.completionTime, other.completionTime)
      .append(this.location, other.location)
      .append(this.eventType, other.eventType)
      .isEquals()

}
