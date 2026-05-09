package se.citerus.dddsample.domain.shared

abstract class AbstractSpecification[T] extends Specification[T] {

  /**
   * { @inheritDoc }
   */
  def isSatisfiedBy(t: T): Boolean

  /**
   * { @inheritDoc }
   */
  def and(specification: Specification[T]): Specification[T] =
    new AndSpecification[T](this, specification)

  /**
   * { @inheritDoc }
   */
  def or(specification: Specification[T]): Specification[T] =
    new OrSpecification[T](this, specification)

  /**
   * { @inheritDoc }
   */
  def not(specification: Specification[T]): Specification[T] =
    new NotSpecification[T](specification)

}
