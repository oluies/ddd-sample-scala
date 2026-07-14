package se.citerus.dddsample.application.impl

import java.util.Objects

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import se.citerus.dddsample.application.{ApplicationEvents, CargoInspectionService}
import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository
import se.citerus.dddsample.domain.shared.DomainError

@Service
final class CargoInspectionServiceImpl(
    applicationEvents: ApplicationEvents,
    cargoRepository: CargoRepository,
    handlingEventRepository: HandlingEventRepository
) extends CargoInspectionService:

  private val logger = LoggerFactory.getLogger(getClass)

  @Transactional
  override def inspectCargo(trackingId: TrackingId): Either[DomainError, Unit] =
    Objects.requireNonNull(trackingId, "Tracking ID is required")
    cargoRepository
      .find(trackingId)
      .toRight {
        logger.warn("Can't inspect non-existing cargo {}", trackingId.idString)
        DomainError.UnknownCargo(trackingId)
      }
      .map { cargo =>
        val handlingHistory = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId)
        val updated         = cargo.deriveDeliveryProgress(handlingHistory)
        if updated.delivery.isMisdirected then applicationEvents.cargoWasMisdirected(updated)
        if updated.delivery.isUnloadedAtDestination then applicationEvents.cargoHasArrived(updated)
        cargoRepository.store(updated)
      }
