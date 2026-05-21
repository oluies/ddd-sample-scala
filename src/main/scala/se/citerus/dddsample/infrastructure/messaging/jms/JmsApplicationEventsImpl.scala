package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.Destination

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.core.{JmsOperations, MessageCreator}

import se.citerus.dddsample.application.ApplicationEvents
import se.citerus.dddsample.domain.model.cargo.Cargo
import se.citerus.dddsample.domain.model.handling.HandlingEvent
import se.citerus.dddsample.interfaces.handling.HandlingEventRegistrationAttempt

/**
 * JMS-backed [[ApplicationEvents]]. Each event becomes a message on a
 * dedicated queue. Used in the default Spring profile; in-process tests
 * can substitute a simpler synchronous implementation.
 *
 * All five queues now carry only `TextMessage` payloads — the four
 * cargo-status queues with a bare tracking-id string, and the registration
 * queue with a JSON-serialised `HandlingEventRegistrationAttempt`. No
 * Java-serialised `ObjectMessage` is ever sent, which means the broker's
 * `setTrustedPackages` list is empty and there's no Java deserialization
 * gadget chain reachable even if an attacker gained broker access.
 */
final class JmsApplicationEventsImpl(
    jmsOperations: JmsOperations,
    cargoHandledQueue: Destination,
    misdirectedCargoQueue: Destination,
    deliveredCargoQueue: Destination,
    @scala.annotation.unused rejectedRegistrationAttemptsQueue: Destination,
    handlingEventQueue: Destination,
    objectMapper: ObjectMapper
) extends ApplicationEvents:

  private val logger = LoggerFactory.getLogger(getClass)

  override def cargoWasHandled(event: HandlingEvent): Unit =
    val cargo = event.cargo
    logger.info("Cargo was handled {}", cargo)
    jmsOperations.send(
      cargoHandledQueue,
      (session => session.createTextMessage(cargo.trackingId.idString)): MessageCreator
    )

  override def cargoWasMisdirected(cargo: Cargo): Unit =
    logger.info("Cargo was misdirected {}", cargo)
    jmsOperations.send(
      misdirectedCargoQueue,
      (session => session.createTextMessage(cargo.trackingId.idString)): MessageCreator
    )

  override def cargoHasArrived(cargo: Cargo): Unit =
    logger.info("Cargo has arrived {}", cargo)
    jmsOperations.send(
      deliveredCargoQueue,
      (session => session.createTextMessage(cargo.trackingId.idString)): MessageCreator
    )

  override def receivedHandlingEventRegistrationAttempt(
      attempt: HandlingEventRegistrationAttempt
  ): Unit =
    logger.info("Received handling event registration attempt {}", attempt)
    val payload = objectMapper.writeValueAsString(attempt)
    jmsOperations.send(
      handlingEventQueue,
      (session => session.createTextMessage(payload)): MessageCreator
    )
