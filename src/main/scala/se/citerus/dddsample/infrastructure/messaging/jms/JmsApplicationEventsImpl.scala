package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.Destination
import org.slf4j.LoggerFactory
import org.springframework.jms.core.{JmsOperations, MessageCreator}

import se.citerus.dddsample.application.ApplicationEvents
import se.citerus.dddsample.domain.model.cargo.Cargo
import se.citerus.dddsample.domain.model.handling.HandlingEvent
import se.citerus.dddsample.interfaces.handling.HandlingEventRegistrationAttempt

/** JMS-backed [[ApplicationEvents]]. Each event becomes a message on a
  * dedicated queue. Used in the default Spring profile; in-process tests
  * can substitute a simpler synchronous implementation.
  */
final class JmsApplicationEventsImpl(
    jmsOperations: JmsOperations,
    cargoHandledQueue: Destination,
    misdirectedCargoQueue: Destination,
    deliveredCargoQueue: Destination,
    @scala.annotation.unused rejectedRegistrationAttemptsQueue: Destination,
    handlingEventQueue: Destination
) extends ApplicationEvents:

  private val logger = LoggerFactory.getLogger(getClass)

  override def cargoWasHandled(event: HandlingEvent): Unit =
    val cargo = event.cargo
    logger.info("Cargo was handled {}", cargo)
    jmsOperations.send(cargoHandledQueue,
      (session => session.createTextMessage(cargo.trackingId.idString)): MessageCreator)

  override def cargoWasMisdirected(cargo: Cargo): Unit =
    logger.info("Cargo was misdirected {}", cargo)
    jmsOperations.send(misdirectedCargoQueue,
      (session => session.createTextMessage(cargo.trackingId.idString)): MessageCreator)

  override def cargoHasArrived(cargo: Cargo): Unit =
    logger.info("Cargo has arrived {}", cargo)
    jmsOperations.send(deliveredCargoQueue,
      (session => session.createTextMessage(cargo.trackingId.idString)): MessageCreator)

  override def receivedHandlingEventRegistrationAttempt(attempt: HandlingEventRegistrationAttempt): Unit =
    logger.info("Received handling event registration attempt {}", attempt)
    jmsOperations.send(handlingEventQueue,
      (session => session.createObjectMessage(attempt)): MessageCreator)
