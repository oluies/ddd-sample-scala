package se.citerus.dddsample.interfaces.booking.facade

import java.time.Instant

import se.citerus.dddsample.domain.shared.DomainError
import se.citerus.dddsample.interfaces.booking.facade.dto.{
  CargoRoutingDTO,
  LocationDTO,
  RouteCandidateDTO
}

/**
 * Shields the domain layer from UI / remoting concerns. All arguments and
 * results are primitive Strings or DTOs.
 *
 * Operations that can fail on the domain happy path return
 * `Either[DomainError, A]`; the controller maps each case to an HTTP status.
 * Pure listings (locations, cargos) still return raw lists — they only fail
 * for infrastructure reasons, which stay as thrown exceptions.
 */
trait BookingServiceFacade:

  def bookNewCargo(
      origin: String,
      destination: String,
      arrivalDeadline: Instant
  ): Either[DomainError, String]

  def loadCargoForRouting(trackingId: String): Either[DomainError, CargoRoutingDTO]

  def assignCargoToRoute(
      trackingId: String,
      route: RouteCandidateDTO
  ): Either[DomainError, Unit]

  def changeDestination(
      trackingId: String,
      destinationUnLocode: String
  ): Either[DomainError, Unit]

  def requestPossibleRoutesForCargo(
      trackingId: String
  ): Either[DomainError, List[RouteCandidateDTO]]

  def listShippingLocations(): List[LocationDTO]

  def listAllCargos(): List[CargoRoutingDTO]
