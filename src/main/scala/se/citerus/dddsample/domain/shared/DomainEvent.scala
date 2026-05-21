package se.citerus.dddsample.domain.shared

/**
 * A domain event is something that is unique, but does not have a lifecycle.
 *
 * The identity may be explicit (e.g. the sequence number of a payment) or
 * derived from various aspects of the event such as where, when and what has
 * happened.
 */
trait DomainEvent[T]:
  /**
   * @param other The other domain event.
   * @return true if this and `other` are regarded as being the same event.
   */
  def sameEventAs(other: T): Boolean
