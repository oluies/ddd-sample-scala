package se.citerus.dddsample.domain.shared

/** OR specification: satisfied iff at least one operand is satisfied. */
final class OrSpecification[T](spec1: Specification[T], spec2: Specification[T])
    extends AbstractSpecification[T]:
  override def isSatisfiedBy(t: T): Boolean =
    spec1.isSatisfiedBy(t) || spec2.isSatisfiedBy(t)
