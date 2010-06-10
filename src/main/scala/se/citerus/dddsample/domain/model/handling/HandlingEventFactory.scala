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
