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
import se.citerus.dddsample.domain.shared.DomainError

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
  ): Either[DomainError, TrackingId] =
    cargoFactory.createCargo(origin, destination, arrivalDeadline).map { cargo =>
      cargoRepository.store(cargo)
      logger.info("Booked new cargo with tracking id {}", cargo.trackingId.idString)
      cargo.trackingId
    }

  @Transactional
  override def requestPossibleRoutesForCargo(
      trackingId: TrackingId
  ): Either[DomainError, List[Itinerary]] =
    cargoRepository
      .find(trackingId)
      .toRight(DomainError.UnknownCargo(trackingId))
      .map(cargo => routingService.fetchRoutesForSpecification(cargo.routeSpecification))

  @Transactional
  override def assignCargoToRoute(
      itinerary: Itinerary,
      trackingId: TrackingId
  ): Either[DomainError, Unit] =
    cargoRepository
      .find(trackingId)
      .toRight(DomainError.UnknownCargo(trackingId))
      .map { cargo =>
        cargoRepository.store(cargo.assignToRoute(itinerary))
        logger.info("Assigned cargo {} to new route", trackingId.idString)
      }

  @Transactional
  override def changeDestination(
      trackingId: TrackingId,
      unLocode: UnLocode
  ): Either[DomainError, Unit] =
    for
      cargo <- cargoRepository.find(trackingId).toRight(DomainError.UnknownCargo(trackingId))
      newDestination <- locationRepository
        .find(unLocode)
        .toRight(DomainError.UnknownLocation(unLocode))
      newSpec <- buildNewSpec(
        cargo.origin,
        newDestination,
        cargo.routeSpecification.arrivalDeadline
      )
    yield
      cargoRepository.store(cargo.specifyNewRoute(newSpec))
      logger.info(
        "Changed destination for cargo {} to {}",
        trackingId.idString,
        newSpec.destination
      )

  private def buildNewSpec(
      origin: se.citerus.dddsample.domain.model.location.Location,
      destination: se.citerus.dddsample.domain.model.location.Location,
      deadline: Instant
  ): Either[DomainError, RouteSpecification] =
    try Right(RouteSpecification(origin, destination, deadline))
    catch case e: IllegalArgumentException => Left(DomainError.fromThrowable(e))
