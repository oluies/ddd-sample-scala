package se.citerus.dddsample.interfaces.booking.facade.internal

import java.util.Date

import scala.beans.BeanProperty

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import se.citerus.dddsample.application._
import se.citerus.dddsample.domain.model.cargo._
import se.citerus.dddsample.domain.model.location._
import se.citerus.dddsample.domain.model.voyage._
import se.citerus.dddsample.interfaces.booking.facade.BookingServiceFacade
import se.citerus.dddsample.interfaces.booking.facade.dto._
import se.citerus.dddsample.interfaces.booking.facade.internal.assembler._

class BookingServiceFacadeImpl extends BookingServiceFacade {
  var bookingService: BookingService         = _
  var locationRepository: LocationRepository = _
  var cargoRepository: CargoRepository       = _
  var voyageRepository: VoyageRepository     = _

  val logger = LogFactory.getLog(getClass());

  override def listShippingLocations(): List[LocationDTO] = {
    val allLocations = locationRepository.findAll()
    LocationDTOAssembler.toDTOList(allLocations)
  }

  def bookNewCargo(origin: String, destination: String, arrivalDeadline: Date): String = {
    val trackingId = bookingService.bookNewCargo(
      new UnLocode(origin),
      new UnLocode(destination),
      arrivalDeadline
    );
    return trackingId.idString;
  }

  def loadCargoForRouting(trackingId: String): CargoRoutingDTO = {
    val o: Option[Cargo] = cargoRepository.find(new TrackingId(trackingId))
    val cargo: Cargo     = o.getOrElse(throw new RuntimeException("Not found"));
    return CargoRoutingDTOAssembler.toDTO(cargo);
  }

  def assignCargoToRoute(trackingIdStr: String, routeCandidateDTO: RouteCandidateDTO) = {
    val itinerary = ItineraryCandidateDTOAssembler.fromDTO(
      routeCandidateDTO,
      voyageRepository,
      locationRepository
    );
    val trackingId = new TrackingId(trackingIdStr);

    bookingService.assignCargoToRoute(itinerary, trackingId);
  }

  def changeDestination(trackingId: String, destinationUnLocode: String) =
    bookingService.changeDestination(new TrackingId(trackingId), new UnLocode(destinationUnLocode));

  def listAllCargos(): List[CargoRoutingDTO] = {
    val cargoList                      = cargoRepository.findAll();
    var dtoList: List[CargoRoutingDTO] = List();
    for (cargo <- cargoList)
      dtoList = CargoRoutingDTOAssembler.toDTO(cargo) :: dtoList
    return dtoList;
  }

  def requestPossibleRoutesForCargo(trackingId: String): List[RouteCandidateDTO] = {
    val itineraries: List[Itinerary] =
      bookingService.requestPossibleRoutesForCargo(new TrackingId(trackingId));

    var routeCandidates: List[RouteCandidateDTO] = List()
    for (itinerary <- itineraries)
      routeCandidates = ItineraryCandidateDTOAssembler.toDTO(itinerary) :: routeCandidates

    return routeCandidates;
  }

}
