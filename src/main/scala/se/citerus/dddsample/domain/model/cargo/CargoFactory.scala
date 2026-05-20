package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}

/**
 * Application-tier factory that creates a fresh [[Cargo]] aggregate, looking
 * up origin and destination [[se.citerus.dddsample.domain.model.location.Location]]s
 * via [[LocationRepository]] and minting a new [[TrackingId]] via
 * [[CargoRepository.nextTrackingId]].
 *
 * Throws `NoSuchElementException` if either UN/Locode is unknown (upstream
 * Java silently passes `null` through; we surface the error instead).
 */
final class CargoFactory(
    locationRepository: LocationRepository,
    cargoRepository: CargoRepository
):

  def createCargo(
      originUnLoCode: UnLocode,
      destinationUnLoCode: UnLocode,
      arrivalDeadline: Instant
  ): Cargo =
    val trackingId = cargoRepository.nextTrackingId()
    val origin = locationRepository
      .find(originUnLoCode)
      .getOrElse(
        throw new NoSuchElementException(s"Unknown origin UN locode: ${originUnLoCode.idString}")
      )
    val destination = locationRepository
      .find(destinationUnLoCode)
      .getOrElse(
        throw new NoSuchElementException(
          s"Unknown destination UN locode: ${destinationUnLoCode.idString}"
        )
      )
    val spec = RouteSpecification(origin, destination, arrivalDeadline)
    new Cargo(trackingId, spec)
