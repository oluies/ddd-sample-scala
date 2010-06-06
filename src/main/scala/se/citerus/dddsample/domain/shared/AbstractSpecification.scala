package se.citerus.dddsample.domain.shared

abstract class AbstractSpecification[T] extends Specification[T] {

  /**
   * {@inheritDoc}
   */
  def isSatisfiedBy(t:T) : Boolean

  /**
   * {@inheritDoc}
   */
  def and(specification:Specification[T]) : Specification[T] = {
    return new AndSpecification[T](this, specification);
  }

  /**
   * {@inheritDoc}
   */
  def or(specification:Specification[T]) : Specification[T] = {
    return new OrSpecification[T](this, specification);
  }

  /**
   * {@inheritDoc}
   */
  def not(specification:Specification[T]) : Specification[T] = {
    return new NotSpecification[T](specification);
  }
  
}