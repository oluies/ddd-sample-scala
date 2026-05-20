package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.{Message, MessageListener, ObjectMessage}
import org.slf4j.LoggerFactory

import se.citerus.dddsample.application.HandlingEventService
import se.citerus.dddsample.interfaces.handling.HandlingEventRegistrationAttempt

/** Consumes registration-attempt messages off the queue and feeds them into
  * the [[HandlingEventService]].
  */
final class HandlingEventRegistrationAttemptConsumer(handlingEventService: HandlingEventService) extends MessageListener:

  private val logger = LoggerFactory.getLogger(getClass)

  override def onMessage(message: Message): Unit =
    try
      val om      = message.asInstanceOf[ObjectMessage]
      val attempt = om.getObject.asInstanceOf[HandlingEventRegistrationAttempt]
      handlingEventService.registerHandlingEvent(
        attempt.completionTime,
        attempt.trackingId,
        attempt.voyageNumber,
        attempt.unLocode,
        attempt.eventType
      )
    catch
      case e: Exception => logger.error("Error consuming HandlingEventRegistrationAttempt message", e)
