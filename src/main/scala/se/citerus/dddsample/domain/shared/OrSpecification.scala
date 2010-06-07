package se.citerus.dddsample.domain.shared

;

/**
 * OR specification, used to create a new specification that is the OR of two other specifications.
 */
class OrSpecification[T](val spec1: Specification[T], val spec2: Specification[T]) extends AbstractSpecification[T] {

  /**
   * { @inheritDoc }
   */
  def isSatisfiedBy(t: T): Boolean = {
    return spec1.isSatisfiedBy(t) || spec2.isSatisfiedBy(t);
  }
}
