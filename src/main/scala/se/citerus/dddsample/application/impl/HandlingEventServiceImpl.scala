package se.citerus.dddsample.application.impl

import java.time.Instant

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import se.citerus.dddsample.application.{ApplicationEvents, HandlingEventService}
import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.{
  HandlingEventFactory,
  HandlingEventRepository,
  HandlingEventType
}
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber
import se.citerus.dddsample.domain.shared.DomainError

@Service
final class HandlingEventServiceImpl(
    handlingEventRepository: HandlingEventRepository,
    applicationEvents: ApplicationEvents,
    handlingEventFactory: HandlingEventFactory
) extends HandlingEventService:

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * No `rollbackFor` override needed any more: the method no longer throws.
   * The factory's `Left` short-circuits before any side-effect, so there's
   * nothing to roll back.
   */
  @Transactional
  override def registerHandlingEvent(
      completionTime: Instant,
      trackingId: TrackingId,
      voyageNumber: Option[VoyageNumber],
      unLocode: UnLocode,
      eventType: HandlingEventType
  ): Either[DomainError, Unit] =
    val registrationTime = Instant.now()
    handlingEventFactory
      .createHandlingEvent(
        registrationTime,
        completionTime,
        trackingId,
        voyageNumber,
        unLocode,
        eventType
      )
      .map { event =>
        handlingEventRepository.store(event)
        applicationEvents.cargoWasHandled(event)
        logger.info("Registered handling event: {}", event)
      }
