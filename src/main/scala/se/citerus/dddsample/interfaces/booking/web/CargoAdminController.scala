package se.citerus.dddsample.interfaces.booking.web

import java.time.Instant

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
  def show(@PathVariable trackingId: String): CargoRoutingDTO =
    facade.loadCargoForRouting(trackingId)

  @PostMapping(path = Array("/cargos"))
  def register(@RequestBody command: RegistrationCommand): ResponseEntity[Map[String, String]] =
    val trackingId = facade.bookNewCargo(
      command.originUnlocode,
      command.destinationUnlocode,
      Instant.parse(command.arrivalDeadline)
    )
    ResponseEntity.ok(Map("trackingId" -> trackingId))

  @GetMapping(path = Array("/cargos/{trackingId}/route-candidates"))
  def routeCandidates(@PathVariable trackingId: String): List[RouteCandidateDTO] =
    facade.requestPossibleRoutesForCargo(trackingId)

  @PostMapping(path = Array("/cargos/{trackingId}/route"))
  def assignItinerary(
      @PathVariable trackingId: String,
      @RequestBody route: RouteCandidateDTO
  ): ResponseEntity[Void] =
    facade.assignCargoToRoute(trackingId, route)
    ResponseEntity.noContent().build()

  @PostMapping(path = Array("/cargos/{trackingId}/destination"))
  def changeDestination(
      @PathVariable trackingId: String,
      @RequestParam unlocode: String
  ): ResponseEntity[Void] =
    facade.changeDestination(trackingId, unlocode)
    ResponseEntity.noContent().build()
