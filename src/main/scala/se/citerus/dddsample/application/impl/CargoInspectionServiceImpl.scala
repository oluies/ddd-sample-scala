package se.citerus.dddsample.application.impl

import java.util.Objects

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import se.citerus.dddsample.application.{ApplicationEvents, CargoInspectionService}
import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository

@Service
final class CargoInspectionServiceImpl(
    applicationEvents: ApplicationEvents,
    cargoRepository: CargoRepository,
    handlingEventRepository: HandlingEventRepository
) extends CargoInspectionService:

  private val logger = LoggerFactory.getLogger(getClass)

  @Transactional
  override def inspectCargo(trackingId: TrackingId): Unit =
    Objects.requireNonNull(trackingId, "Tracking ID is required")
    cargoRepository.find(trackingId) match
      case None =>
        logger.warn("Can't inspect non-existing cargo {}", trackingId.idString)
      case Some(cargo) =>
        val handlingHistory = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId)
        cargo.deriveDeliveryProgress(handlingHistory)
        if cargo.delivery.isMisdirected then applicationEvents.cargoWasMisdirected(cargo)
        if cargo.delivery.isUnloadedAtDestination then applicationEvents.cargoHasArrived(cargo)
        cargoRepository.store(cargo)
