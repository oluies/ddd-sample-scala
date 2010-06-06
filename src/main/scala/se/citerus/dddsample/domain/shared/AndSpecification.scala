package se.citerus.dddsample.domain.shared;

/**
 * AND specification, used to create a new specifcation that is the AND of two other specifications.
 */
class AndSpecification[T](val spec1:Specification[T], val spec2:Specification[T]) extends AbstractSpecification[T] {

  /**
   * {@inheritDoc}
   */
  def isSatisfiedBy(t:T) : Boolean = {
      return spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t);
  }
}
