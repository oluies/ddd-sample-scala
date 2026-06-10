package se.citerus.dddsample.interfaces.booking.web

import java.time.Instant

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import se.citerus.dddsample.interfaces.DomainErrorResponses
import se.citerus.dddsample.interfaces.booking.facade.BookingServiceFacade
import se.citerus.dddsample.interfaces.booking.facade.dto.{
  CargoRoutingDTO,
  LocationDTO,
  RouteCandidateDTO
}

/**
 * REST controller for cargo administration. Upstream Java renders JSP views;
 * Decision D5 swaps that for JSON via Spring Web MVC + Jackson, so this is a
 * `@RestController` returning DTOs / `ResponseEntity`.
 *
 * Each operation `.fold`s over the facade's `Either[DomainError, A]` —
 * `Left` becomes the appropriate HTTP status via [[DomainErrorResponses]],
 * `Right` becomes the success payload.
 */
@RestController
@RequestMapping(path = Array("/admin"))
final class CargoAdminController(facade: BookingServiceFacade):

  @GetMapping(path = Array("/locations"))
  def listLocations(): List[LocationDTO] =
    facade.listShippingLocations()

  @GetMapping(path = Array("/cargos"))
  def listAllCargos(): List[CargoRoutingDTO] =
    facade.listAllCargos()

  @GetMapping(path = Array("/cargos/{trackingId}"))
  def show(@PathVariable trackingId: String): ResponseEntity[?] =
    facade
      .loadCargoForRouting(trackingId)
      .fold(DomainErrorResponses.toAnyResponse, ResponseEntity.ok(_))

  @PostMapping(path = Array("/cargos"))
  def register(@RequestBody command: RegistrationCommand): ResponseEntity[?] =
    facade
      .bookNewCargo(
        command.originUnlocode,
        command.destinationUnlocode,
        Instant.parse(command.arrivalDeadline)
      )
      .fold(
        DomainErrorResponses.toAnyResponse,
        trackingId => ResponseEntity.ok(Map("trackingId" -> trackingId))
      )

  @GetMapping(path = Array("/cargos/{trackingId}/route-candidates"))
  def routeCandidates(@PathVariable trackingId: String): ResponseEntity[?] =
    facade
      .requestPossibleRoutesForCargo(trackingId)
      .fold(DomainErrorResponses.toAnyResponse, ResponseEntity.ok(_))

  @PostMapping(path = Array("/cargos/{trackingId}/route"))
  def assignItinerary(
      @PathVariable trackingId: String,
      @RequestBody route: RouteCandidateDTO
  ): ResponseEntity[?] =
    facade
      .assignCargoToRoute(trackingId, route)
      .fold(DomainErrorResponses.toAnyResponse, _ => ResponseEntity.noContent().build())

  @PostMapping(path = Array("/cargos/{trackingId}/destination"))
  def changeDestination(
      @PathVariable trackingId: String,
      @RequestParam unlocode: String
  ): ResponseEntity[?] =
    facade
      .changeDestination(trackingId, unlocode)
      .fold(DomainErrorResponses.toAnyResponse, _ => ResponseEntity.noContent().build())
