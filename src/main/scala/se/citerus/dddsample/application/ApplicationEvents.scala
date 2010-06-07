package se.citerus.dddsample.application

import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.interfaces.handling.HandlingEventRegistrationAttempt;

/**
 * This interface provides a way to let other parts
 * of the system know about events that have occurred.
 * <p/>
 * It may be implemented synchronously or asynchronously, using
 * for example JMS.
 */
trait ApplicationEvents {

  /**
   * A cargo has been handled.
   *
   * @param event handling event
   */
  def cargoWasHandled(event: HandlingEvent): Unit;

  /**
   * A cargo has been misdirected.
   *
   * @param cargo cargo
   */
  def cargoWasMisdirected(cargo: Cargo): Unit;

  /**
   * A cargo has arrived at its final destination.
   *
   * @param cargo cargo
   */
  def cargoHasArrived(cargo: Cargo): Unit;

  /**
   * A handling event registration attempt is received.
   *
   * @param attempt handling event registration attempt
   */
  def receivedHandlingEventRegistrationAttempt(attempt: HandlingEventRegistrationAttempt): Unit;

}
