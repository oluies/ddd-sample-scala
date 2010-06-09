package se.citerus.dddsample.interfaces.booking.facade.internal

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory;

import scala.reflect.BeanProperty
import se.citerus.dddsample.interfaces.booking.facade.BookingServiceFacade
import java.util.Date
import se.citerus.dddsample.interfaces.booking.facade.dto.{LocationDTO, RouteCandidateDTO, CargoRoutingDTO}
import se.citerus.dddsample.domain.model.cargo._;
import se.citerus.dddsample.domain.model.voyage._;
import se.citerus.dddsample.domain.model.location._;
import se.citerus.dddsample.application._;


class BookingServiceFacadeImpl extends BookingServiceFacade {
  @BeanProperty var bookingService:BookingService;
  @BeanProperty var locationRepository:LocationRepository;
  @BeanProperty var cargoRepository:CargoRepository;
  @BeanProperty var voyageRepository:VoyageRepository;
  
  val logger = LogFactory.getLog(getClass());
  
  override def listShippingLocations : List[LocationDTO] = {
    val allLocations = locationRepository.findAll();
    return LocationDTOAssembler.toDTOList(allLocations);
  }

  def bookNewCargo(origin:String, destination:String, arrivalDeadline:Date) : String = {
    val trackingId = bookingService.bookNewCargo(
      new UnLocode(origin), 
      new UnLocode(destination),
      arrivalDeadline
    );
    return trackingId.idString;
  }

 def loadCargoForRouting(trackingId:String) :CargoRoutingDTO = {
    val cargo = cargoRepository.find(new TrackingId(trackingId));
    return CargoRoutingDTOAssembler.toDTO(cargo);
  }

  def assignCargoToRoute(trackingIdStr:String, routeCandidateDTO:RouteCandidateDTO) = {
    val itinerary = ItineraryCandidateDTOAssembler.fromDTO(routeCandidateDTO, voyageRepository, locationRepository);
    val trackingId = new TrackingId(trackingIdStr);

    bookingService.assignCargoToRoute(itinerary, trackingId);
  }

  def changeDestination(trackingId:String, destinationUnLocode:String) = {
    bookingService.changeDestination(new TrackingId(trackingId), new UnLocode(destinationUnLocode));
  }

  def listAllCargos() : List[CargoRoutingDTO] = {
    val cargoList = cargoRepository.findAll();
    var dtoList:List[CargoRoutingDTO] = List();
    for (val cargo <- cargoList) {
      dtoList = CargoRoutingDTOAssembler.toDTO(cargo) :: dtoList
    }
    return dtoList;
  }

  def requestPossibleRoutesForCargo(trackingId:String) :  List[RouteCandidateDTO] = {
    val itineraries:List[Itinerary] = bookingService.requestPossibleRoutesForCargo(new TrackingId(trackingId));

    val routeCandidates:List[RouteCandidateDTO] = List()
    for (itinerary <- itineraries) {
      routeCandidates = ItineraryCandidateDTOAssembler.toDTO(itinerary) :: routeCandidates
    }

    return routeCandidates;
  }

}
