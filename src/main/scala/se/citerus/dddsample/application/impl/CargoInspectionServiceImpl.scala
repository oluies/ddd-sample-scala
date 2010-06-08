package se.citerus.dddsample.application.impl

import org.apache.commons.logging.{LogFactory, Log}

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import se.citerus.dddsample.application.ApplicationEvents;
import se.citerus.dddsample.application.CargoInspectionService;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;
import se.citerus.dddsample.domain.model.handling.HandlingHistory;
              
class CargoInspectionServiceImpl(
  val applicationEvents:ApplicationEvents,
  val cargoRepository:CargoRepository,
  val handlingEventRepository:HandlingEventRepository) extends CargoInspectionService {

  val logger = LogFactory.getLog(getClass());

  @Override
  @Transactional
  def inspectCargo(trackingId:TrackingId) : Unit = {
    Validate.notNull(trackingId, "Tracking ID is required");

    val cargo = cargoRepository.find(trackingId).getOrElse {
      logger.warn("Can't inspect non-existing cargo " + trackingId);
      return;
    }

    val handlingHistory = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId);

    cargo.deriveDeliveryProgress(handlingHistory);

    if (cargo.delivery.misdirected) {
      applicationEvents.cargoWasMisdirected(cargo);
    }

    if (cargo.delivery.unloadedAtDestination) {
      applicationEvents.cargoHasArrived(cargo);
    }

    cargoRepository.store(cargo);
  }
}