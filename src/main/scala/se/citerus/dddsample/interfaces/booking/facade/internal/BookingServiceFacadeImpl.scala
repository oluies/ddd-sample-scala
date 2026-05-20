package se.citerus.dddsample.interfaces.booking.facade.internal

import java.time.Instant

import se.citerus.dddsample.application.BookingService
import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.VoyageRepository
import se.citerus.dddsample.interfaces.booking.facade.BookingServiceFacade
import se.citerus.dddsample.interfaces.booking.facade.dto.{
  CargoRoutingDTO,
  LocationDTO,
  RouteCandidateDTO
}
import se.citerus.dddsample.interfaces.booking.facade.internal.assembler.{
  CargoRoutingDTOAssembler,
  ItineraryCandidateDTOAssembler,
  LocationDTOAssembler
}

final class BookingServiceFacadeImpl(
    bookingService: BookingService,
    locationRepository: LocationRepository,
    cargoRepository: CargoRepository,
    voyageRepository: VoyageRepository
) extends BookingServiceFacade:

  private val locationAssembler  = new LocationDTOAssembler
  private val cargoAssembler     = new CargoRoutingDTOAssembler
  private val itineraryAssembler = new ItineraryCandidateDTOAssembler

  override def listShippingLocations(): List[LocationDTO] =
    locationAssembler.toDTOList(locationRepository.getAll())

  override def bookNewCargo(origin: String, destination: String, arrivalDeadline: Instant): String =
    bookingService.bookNewCargo(UnLocode(origin), UnLocode(destination), arrivalDeadline).idString

  override def loadCargoForRouting(trackingId: String): CargoRoutingDTO =
    val cargo = cargoRepository
      .find(TrackingId(trackingId))
      .getOrElse(
        throw new NoSuchElementException(s"Unknown cargo $trackingId")
      )
    cargoAssembler.toDTO(cargo)

  override def assignCargoToRoute(trackingIdStr: String, route: RouteCandidateDTO): Unit =
    val itinerary = itineraryAssembler.fromDTO(route, voyageRepository, locationRepository)
    bookingService.assignCargoToRoute(itinerary, TrackingId(trackingIdStr))

  override def changeDestination(trackingId: String, destinationUnLocode: String): Unit =
    bookingService.changeDestination(TrackingId(trackingId), UnLocode(destinationUnLocode))

  override def listAllCargos(): List[CargoRoutingDTO] =
    cargoRepository.getAll.map(cargoAssembler.toDTO)

  override def requestPossibleRoutesForCargo(trackingId: String): List[RouteCandidateDTO] =
    bookingService
      .requestPossibleRoutesForCargo(TrackingId(trackingId))
      .map(itineraryAssembler.toDTO)
