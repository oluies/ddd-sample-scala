package se.citerus.dddsample.domain.shared

import java.io.Serializable

/**
 * A value object, as described in the DDD book.
 *
 * Value objects compare by the values of their attributes, they don't have an
 * identity.
 */
trait ValueObject[T] extends Serializable:
  /**
   * @param other The other value object.
   * @return true if this and `other` have the same attribute values.
   */
  def sameValueAs(other: T): Boolean
