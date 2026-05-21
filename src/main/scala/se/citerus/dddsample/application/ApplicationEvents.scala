package se.citerus.dddsample.application

import se.citerus.dddsample.application.handling.HandlingEventRegistrationAttempt
import se.citerus.dddsample.domain.model.cargo.Cargo
import se.citerus.dddsample.domain.model.handling.HandlingEvent

/**
 * A way to let other parts of the system know about events that have
 * occurred. Implementations may be synchronous or asynchronous (e.g. via
 * JMS). Concrete adapter lives in `infrastructure.messaging` (phase 15).
 */
trait ApplicationEvents:

  def cargoWasHandled(event: HandlingEvent): Unit

  def cargoWasMisdirected(cargo: Cargo): Unit

  def cargoHasArrived(cargo: Cargo): Unit

  def receivedHandlingEventRegistrationAttempt(attempt: HandlingEventRegistrationAttempt): Unit
