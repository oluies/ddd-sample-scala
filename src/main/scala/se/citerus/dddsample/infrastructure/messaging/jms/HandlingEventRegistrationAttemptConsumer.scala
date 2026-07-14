package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.{Message, MessageListener, TextMessage}

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

import se.citerus.dddsample.application.HandlingEventService
import se.citerus.dddsample.application.handling.HandlingEventRegistrationAttempt

/**
 * Consumes registration-attempt messages off the queue and feeds them into
 * the [[HandlingEventService]].
 *
 * Payload is JSON (`TextMessage`), not Java-serialised `ObjectMessage`, so
 * no class names cross the wire and ActiveMQ's `setTrustedPackages` can be
 * left empty.
 *
 * The service returns `Either[DomainError, Unit]`. A `Left` (unknown cargo /
 * location / voyage in the payload) is logged at warn level rather than
 * rethrown — rethrowing would put the message into endless retry loops.
 */
final class HandlingEventRegistrationAttemptConsumer(
    handlingEventService: HandlingEventService,
    objectMapper: ObjectMapper
) extends MessageListener:

  private val logger = LoggerFactory.getLogger(getClass)

  override def onMessage(message: Message): Unit =
    try
      val text    = message.asInstanceOf[TextMessage].getText
      val attempt = objectMapper.readValue(text, classOf[HandlingEventRegistrationAttempt])
      handlingEventService
        .registerHandlingEvent(
          attempt.completionTime,
          attempt.trackingId,
          attempt.voyageNumber,
          attempt.unLocode,
          attempt.eventType
        )
        .left
        .foreach(err =>
          logger.warn("Rejecting handling event registration attempt: {}", err.message)
        )
    catch
      case e: Exception =>
        logger.error("Error consuming HandlingEventRegistrationAttempt message", e)
