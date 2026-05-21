package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.{Message, MessageListener}

import org.slf4j.LoggerFactory

/**
 * Drops all received messages onto a debug-level log line. Used for the
 * MisdirectedCargo / DeliveredCargo / RejectedRegistrationAttempts queues
 * where the teaching example just wants visible side-effects.
 */
final class SimpleLoggingConsumer extends MessageListener:
  private val logger = LoggerFactory.getLogger(getClass)
  override def onMessage(message: Message): Unit =
    logger.debug("Received JMS message: {}", message)
