package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.shared.ValueObject;

/**
 * Handling event type. Either requires or prohibits a carrier movement
 * association, it's never optional.
 */
sealed abstract class HandlingEventType(voyageRequired: Boolean) extends ValueObject[HandlingEventType] {
  /**
   * @return True if a voyage association is required for this event type.
   */
  def requiresVoyage(): Boolean = {
    voyageRequired
  }

  /**
   * @return True if a voyage association is prohibited for this event type.
   */
  def prohibitsVoyage(): Boolean = {
    !requiresVoyage()
  }

  override def sameValueAs(other: HandlingEventType): Boolean = {
    other != null && this.equals(other)
  }
}

object HandlingEventType {
  def valueOf(eventTypeString:String) = {
    val eventType:HandlingEventType = eventTypeString match {
      case "RECEIVE" => RECEIVE
      case "LOAD" => LOAD
      case "UNLOAD" => UNLOAD
      case "CLAIM" => CLAIM
      case "CUSTOMS" => CUSTOMS
    }
    eventType
  }
}

case object RECEIVE extends HandlingEventType(voyageRequired = false)
case object LOAD extends HandlingEventType(voyageRequired = true)
case object UNLOAD extends HandlingEventType(voyageRequired = true)
case object CLAIM extends HandlingEventType(voyageRequired = false)
case object CUSTOMS extends HandlingEventType(voyageRequired = false)  