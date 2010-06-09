package se.citerus.dddsample.application.impl

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory;

import se.citerus.dddsample.domain.model.cargo._;
import org.springframework.transaction.annotation.Transactional;
import se.citerus.dddsample.application.BookingService;

import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.service.RoutingService;

import java.util.Date;

class BookingServiceImpl(cargoRepository:CargoRepository,
                         locationRepository:LocationRepository,
                         routingService:RoutingService) extends BookingService {

  private val logger = LogFactory.getLog(getClass());
  
  @Transactional
  def bookNewCargo(originUnLocode:UnLocode,
                   destinationUnLocode:UnLocode,
                   arrivalDeadline:Date ) : TrackingId = {
    val trackingId = cargoRepository.nextTrackingId();
    val origin = locationRepository.find(originUnLocode).getOrElse { throw new IllegalArgumentException("origin not found") }
    val destination = locationRepository.find(destinationUnLocode).getOrElse { throw new IllegalArgumentException("destination not found") };
    val routeSpecification = new RouteSpecification(origin, destination, arrivalDeadline);
    val cargo = new Cargo(trackingId, routeSpecification);

    cargoRepository.store(cargo);
    logger.info("Booked new cargo with tracking id " + cargo.trackingId.idString);

    return cargo.trackingId;
  }

  @Transactional
  def requestPossibleRoutesForCargo(trackingId:TrackingId) : List[Itinerary] = {
    var cargo = cargoRepository.find(trackingId).getOrElse { return List() };

    routingService.fetchRoutesForSpecification(cargo.routeSpecification);
  }

  @Transactional
  def assignCargoToRoute(itinerary:Itinerary, trackingId:TrackingId) {
    val cargo = cargoRepository.find(trackingId).getOrElse { throw new IllegalArgumentException("Can't assign itinerary to non-existing cargo " + trackingId) }

    cargo.assignToRoute(itinerary)
    cargoRepository.store(cargo)

    logger.info("Assigned cargo " + trackingId + " to new route");
  }

  @Transactional
  def changeDestination(trackingId:TrackingId, unLocode:UnLocode) {
    val cargo = cargoRepository.find(trackingId).getOrElse { throw new IllegalArgumentException("Can't find cargo with tracking id = " + trackingId) }
    val newDestination = locationRepository.find(unLocode).getOrElse { throw new IllegalArgumentException("location not found") };

    val routeSpecification = new RouteSpecification(cargo.origin, newDestination, cargo.routeSpecification.arrivalDeadline);
    cargo.specifyNewRoute(routeSpecification);

    cargoRepository.store(cargo);
    logger.info("Changed destination for cargo " + trackingId + " to " + routeSpecification.destination);
  }

}