package se.citerus.dddsample.interfaces.booking.facade

import java.time.Instant

import se.citerus.dddsample.interfaces.booking.facade.dto.{
  CargoRoutingDTO,
  LocationDTO,
  RouteCandidateDTO
}

/** Shields the domain layer from UI / remoting concerns. All arguments and
  * results are primitive Strings or DTOs.
  */
trait BookingServiceFacade:

  def bookNewCargo(origin: String, destination: String, arrivalDeadline: Instant): String

  def loadCargoForRouting(trackingId: String): CargoRoutingDTO

  def assignCargoToRoute(trackingId: String, route: RouteCandidateDTO): Unit

  def changeDestination(trackingId: String, destinationUnLocode: String): Unit

  def requestPossibleRoutesForCargo(trackingId: String): List[RouteCandidateDTO]

  def listShippingLocations(): List[LocationDTO]

  def listAllCargos(): List[CargoRoutingDTO]
