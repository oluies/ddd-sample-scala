package se.citerus.dddsample.domain.model.handling

/**
 * Thrown when a [[HandlingEvent]] cannot be created from a given set of
 * parameters.
 *
 * It is a checked exception in the upstream Java reference because it's not a
 * programming error but rather a special case the application is built to
 * handle — it can occur during normal program execution. Scala doesn't
 * distinguish checked from unchecked exceptions, so this is just a plain
 * `Exception` subclass.
 */
class CannotCreateHandlingEventException(cause: Throwable) extends Exception(cause):
  def this() = this(null)
