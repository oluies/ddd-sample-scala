package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber
import se.citerus.dddsample.domain.shared.DomainEvent

import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.model.voyage.Voyage;
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;

import java.util.Date;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.voyage.Voyage;
import se.citerus.dddsample.domain.shared.DomainEvent;
//import se.citerus.dddsample.domain.shared.DomainObjectUtils;
import se.citerus.dddsample.domain.shared.ValueObject;

import java.util.Date;

/**
 * A HandlingEvent is used to register the event when, for instance,
 * a cargo is unloaded from a carrier at a some loacation at a given time.
 * <p/>
 * The HandlingEvent's are sent from different Incident Logging Applications
 * some time after the event occured and contain information about the
 * { @link se.citerus.dddsample.domain.model.cargo.TrackingId },           { @link se.citerus.dddsample.domain.model.location.Location }, timestamp of the completion of the event,
 * and possibly, if applicable a           { @link se.citerus.dddsample.domain.model.voyage.Voyage }.
 * <p/>
 * This class is the only member, and consequently the root, of the HandlingEvent aggregate. 
 * <p/>
 * HandlingEvent's could contain information about a           { @link Voyage } and if so,
 * the event type must be either           { @link Type # LOAD } or           { @link Type # UNLOAD }.
 * <p/>
 * All other events must be of           { @link Type # RECEIVE },           { @link Type # CLAIM } or           { @link Type # CUSTOMS }.
 */
class HandlingEvent(val cargo: Cargo,
                    val completionTime: Date,
                    val registrationTime: Date,
                    val eventType: HandlingEventType,
                    val location: Location,
                    val voyage: Voyage) extends DomainEvent[HandlingEvent] {
  Validate.notNull(cargo, "Cargo is required");
  Validate.notNull(completionTime, "Completion time is required");
  Validate.notNull(registrationTime, "Registration time is required");
  Validate.notNull(eventType, "Handling event type is required");
  Validate.notNull(location, "Location is required");

  require(!eventType.prohibitsVoyage(), "Voyage is not allowed with event type " + eventType)

  def sameEventAs(other: HandlingEvent): Boolean = {
    other != null && new EqualsBuilder().
            append(this.cargo, other.cargo).
            append(this.voyage, other.voyage).
            append(this.completionTime, other.completionTime).
            append(this.location, other.location).
            append(this.eventType, other.eventType).
            isEquals();
  }

}

/**
 * Creates handling events.
 */
class HandlingEventFactory(
    cargoRepository:CargoRepository,
  voyageRepository:VoyageRepository,
  locationRepository:LocationRepository) {

  /**
   * @param registrationTime time when this event was received by the system
   * @param completionTime when the event was completed, for example finished loading
   * @param trackingId cargo tracking id
   * @param voyageNumber voyage number
   * @param unlocode United Nations Location Code for the location of the event
   * @param type type of event
   * @throws UnknownVoyageException if there's no voyage with this number
   * @throws UnknownCargoException if there's no cargo with this tracking id
   * @throws UnknownLocationException if there's no location with this UN Locode
   * @return A handling event.
   */
  def createHandlingEvent(registrationTime: Date, completionTime: Date, trackingId: TrackingId,
            voyageNumber: VoyageNumber, unlocode: UnLocode, eventType: HandlingEventType): HandlingEvent = {
    val cargo = findCargo(trackingId);
    val voyage = findVoyage(voyageNumber);
    val location = findLocation(unlocode);

    try {
      if (voyage == null) {
        return new HandlingEvent(cargo, completionTime, registrationTime, eventType, location, null);
      } else {
        return new HandlingEvent(cargo, completionTime, registrationTime, eventType, location, voyage);
      }
    } catch {
      case e:Exception => throw new CannotCreateHandlingEventException(e);
    }
  }

  def findCargo(trackingId:TrackingId): Cargo = {
    cargoRepository.find(trackingId).getOrElse { throw new UnknownCargoException(trackingId) }
  }

  def findVoyage(voyageNumber:VoyageNumber) : Voyage = {
    if (voyageNumber == null) {
      return null;
    }

    voyageRepository.find(voyageNumber).getOrElse { throw new UnknownVoyageException(voyageNumber) };
  }

  def findLocation(unlocode: UnLocode): Location = {
    locationRepository.find(unlocode).getOrElse { throw new UnknownLocationException(unlocode) };
  }

}


