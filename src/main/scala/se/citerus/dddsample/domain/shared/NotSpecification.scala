package se.citerus.dddsample.domain.shared;

/**
 * NOT decorator, used to create a new specifcation that is the inverse (NOT) of the given spec.
 */
class NotSpecification[T](val spec1:Specification[T]) extends AbstractSpecification[T] {

  /**
   * {@inheritDoc}
   */
  def isSatisfiedBy(t:T) : Boolean = {
     ! spec1.isSatisfiedBy(t);
  }
}
