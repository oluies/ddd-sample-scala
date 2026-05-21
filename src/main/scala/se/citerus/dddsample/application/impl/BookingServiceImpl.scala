package se.citerus.dddsample.application.impl

import java.time.Instant

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import se.citerus.dddsample.application.BookingService
import se.citerus.dddsample.domain.model.cargo.{
  CargoFactory,
  CargoRepository,
  Itinerary,
  RouteSpecification,
  TrackingId
}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.service.RoutingService

@Service
final class BookingServiceImpl(
    cargoRepository: CargoRepository,
    locationRepository: LocationRepository,
    routingService: RoutingService,
    cargoFactory: CargoFactory
) extends BookingService:

  private val logger = LoggerFactory.getLogger(getClass)

  @Transactional
  override def bookNewCargo(
      origin: UnLocode,
      destination: UnLocode,
      arrivalDeadline: Instant
  ): TrackingId =
    val cargo = cargoFactory.createCargo(origin, destination, arrivalDeadline)
    cargoRepository.store(cargo)
    logger.info("Booked new cargo with tracking id {}", cargo.trackingId.idString)
    cargo.trackingId

  @Transactional
  override def requestPossibleRoutesForCargo(trackingId: TrackingId): List[Itinerary] =
    cargoRepository.find(trackingId) match
      case Some(cargo) => routingService.fetchRoutesForSpecification(cargo.routeSpecification)
      case None        => Nil

  @Transactional
  override def assignCargoToRoute(itinerary: Itinerary, trackingId: TrackingId): Unit =
    val cargo = cargoRepository.find(trackingId).getOrElse {
      throw new IllegalArgumentException(
        s"Can't assign itinerary to non-existing cargo $trackingId"
      )
    }
    cargoRepository.store(cargo.assignToRoute(itinerary))
    logger.info("Assigned cargo {} to new route", trackingId.idString)

  @Transactional
  override def changeDestination(trackingId: TrackingId, unLocode: UnLocode): Unit =
    val cargo = cargoRepository.find(trackingId).getOrElse {
      throw new IllegalArgumentException(s"Unknown cargo $trackingId")
    }
    val newDestination = locationRepository.find(unLocode).getOrElse {
      throw new IllegalArgumentException(s"Unknown destination ${unLocode.idString}")
    }
    val newSpec =
      RouteSpecification(cargo.origin, newDestination, cargo.routeSpecification.arrivalDeadline)
    cargoRepository.store(cargo.specifyNewRoute(newSpec))
    logger.info("Changed destination for cargo {} to {}", trackingId.idString, newSpec.destination)
