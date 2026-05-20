package se.citerus.dddsample.application.impl

import java.time.Instant

import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

import se.citerus.dddsample.application.{ApplicationEvents, HandlingEventService}
import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.{
  CannotCreateHandlingEventException,
  HandlingEventFactory,
  HandlingEventRepository,
  HandlingEventType
}
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber

final class HandlingEventServiceImpl(
    handlingEventRepository: HandlingEventRepository,
    applicationEvents: ApplicationEvents,
    handlingEventFactory: HandlingEventFactory
) extends HandlingEventService:

  private val logger = LoggerFactory.getLogger(getClass)

  @Transactional(rollbackFor = Array(classOf[CannotCreateHandlingEventException]))
  override def registerHandlingEvent(
      completionTime: Instant,
      trackingId: TrackingId,
      voyageNumber: Option[VoyageNumber],
      unLocode: UnLocode,
      eventType: HandlingEventType
  ): Unit =
    val registrationTime = Instant.now()
    val event = handlingEventFactory.createHandlingEvent(
      registrationTime,
      completionTime,
      trackingId,
      voyageNumber,
      unLocode,
      eventType
    )
    handlingEventRepository.store(event)
    applicationEvents.cargoWasHandled(event)
    logger.info("Registered handling event: {}", event)
