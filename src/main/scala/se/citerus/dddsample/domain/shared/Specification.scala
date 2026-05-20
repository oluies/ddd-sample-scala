package se.citerus.dddsample.domain.shared

/**
 * Specification interface.
 *
 * Use [[AbstractSpecification]] as the base for creating specifications;
 * only [[isSatisfiedBy]] needs to be implemented.
 */
trait Specification[T]:

  /**
   * @param t Object to test.
   * @return true if `t` satisfies the specification.
   */
  def isSatisfiedBy(t: T): Boolean

  /** @return a new specification that is the AND of this and `specification`. */
  def and(specification: Specification[T]): Specification[T]

  /** @return a new specification that is the OR of this and `specification`. */
  def or(specification: Specification[T]): Specification[T]

  /** @return a new specification that is the NOT of `specification`. */
  def not(specification: Specification[T]): Specification[T]
