package se.citerus.dddsample.domain.shared

;

/**
 * Specification interface.
 * <p/>
 * Use   { @link se.citerus.dddsample.domain.shared.AbstractSpecification } as base for creating specifications, and
 * only the method   { @link # isSatisfiedBy ( Object ) } must be implemented.
 */
trait Specification[T] {

  /**
   * Check if   { @code t } is satisfied by the specification.
   *
   * @param t Object to test.
   * @return { @code true } if   { @code t } satisfies the specification.
   */
  def isSatisfiedBy(t: T): Boolean;

  /**
   * Create a new specification that is the AND operation of   { @code this } specification and another specification.
   * @param specification Specification to AND.
   * @return A new specification.
   */
  def and(specification: Specification[T]): Specification[T];

  /**
   * Create a new specification that is the OR operation of   { @code this } specification and another specification.
   * @param specification Specification to OR.
   * @return A new specification.
   */
  def or(specification: Specification[T]): Specification[T];

  /**
   * Create a new specification that is the NOT operation of   { @code this } specification.
   * @param specification Specification to NOT.
   * @return A new specification.
   */
  def not(specification: Specification[T]): Specification[T];
}
