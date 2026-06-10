package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.shared.DomainError

/**
 * Application-tier factory that creates a fresh [[Cargo]] aggregate, looking
 * up origin and destination [[se.citerus.dddsample.domain.model.location.Location]]s
 * via [[LocationRepository]] and minting a new [[TrackingId]] via
 * [[CargoRepository.nextTrackingId]].
 *
 * Returns `Either[DomainError, Cargo]` — unknown UN/Locode → `UnknownLocation`,
 * and an `IllegalArgumentException` from `RouteSpecification`'s invariants
 * (e.g. origin == destination) is captured as `InvariantViolation`.
 */
final class CargoFactory(
    locationRepository: LocationRepository,
    cargoRepository: CargoRepository
):

  def createCargo(
      originUnLoCode: UnLocode,
      destinationUnLoCode: UnLocode,
      arrivalDeadline: Instant
  ): Either[DomainError, Cargo] =
    for
      origin <- locationRepository
        .find(originUnLoCode)
        .toRight(DomainError.UnknownLocation(originUnLoCode))
      destination <- locationRepository
        .find(destinationUnLoCode)
        .toRight(DomainError.UnknownLocation(destinationUnLoCode))
      spec <- buildSpec(origin, destination, arrivalDeadline)
    yield Cargo(cargoRepository.nextTrackingId(), spec)

  private def buildSpec(
      origin: se.citerus.dddsample.domain.model.location.Location,
      destination: se.citerus.dddsample.domain.model.location.Location,
      arrivalDeadline: Instant
  ): Either[DomainError, RouteSpecification] =
    try Right(RouteSpecification(origin, destination, arrivalDeadline))
    catch case e: IllegalArgumentException => Left(DomainError.fromThrowable(e))
