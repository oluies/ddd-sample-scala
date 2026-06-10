package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.{Message, MessageListener, TextMessage}

import org.slf4j.LoggerFactory

import se.citerus.dddsample.application.CargoInspectionService
import se.citerus.dddsample.domain.model.cargo.TrackingId

/**
 * Consumes `CargoHandledQueue` messages and delegates to the inspection
 * service. Makes cargo inspection message-driven.
 *
 * The inspection service returns `Either[DomainError, Unit]`. A `Left`
 * (unknown tracking id in the message) is logged at warn level rather than
 * rethrown — rethrowing would loop the message endlessly on the queue.
 */
final class CargoHandledConsumer(cargoInspectionService: CargoInspectionService)
    extends MessageListener:

  private val logger = LoggerFactory.getLogger(getClass)

  override def onMessage(message: Message): Unit =
    try
      val text = message.asInstanceOf[TextMessage].getText
      cargoInspectionService
        .inspectCargo(TrackingId(text))
        .left
        .foreach(err => logger.warn("Cargo inspection failed: {}", err.message))
    catch case e: Exception => logger.error("Error consuming CargoHandled message", e)
