package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.shared.ValueObject

/**
 * Handling event type. Either requires or prohibits a carrier movement
 * association; it's never optional.
 *
 * Upstream Java declares this as a nested enum inside `HandlingEvent`
 * (`HandlingEvent.Type`). Scala 3 enums work best at package level, so this
 * is hoisted out and renamed to `HandlingEventType`. Callers write
 * `HandlingEventType.LOAD` rather than `HandlingEvent.Type.LOAD`.
 */
enum HandlingEventType(val voyageRequired: Boolean) extends ValueObject[HandlingEventType]:
  case LOAD    extends HandlingEventType(true)
  case UNLOAD  extends HandlingEventType(true)
  case RECEIVE extends HandlingEventType(false)
  case CLAIM   extends HandlingEventType(false)
  case CUSTOMS extends HandlingEventType(false)

  def requiresVoyage: Boolean  = voyageRequired
  def prohibitsVoyage: Boolean = !voyageRequired

  override def sameValueAs(other: HandlingEventType): Boolean = this == other
