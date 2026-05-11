package se.citerus.dddsample.domain.shared

/** Abstract base implementation of composite [[Specification]] with default
  * implementations for `and`, `or` and `not`.
  */
abstract class AbstractSpecification[T] extends Specification[T]:

  def isSatisfiedBy(t: T): Boolean

  override def and(specification: Specification[T]): Specification[T] =
    new AndSpecification[T](this, specification)

  override def or(specification: Specification[T]): Specification[T] =
    new OrSpecification[T](this, specification)

  override def not(specification: Specification[T]): Specification[T] =
    new NotSpecification[T](specification)
