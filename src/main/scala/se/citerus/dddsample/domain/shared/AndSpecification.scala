package se.citerus.dddsample.domain.shared

/** AND specification: satisfied iff both operands are satisfied. */
final class AndSpecification[T](spec1: Specification[T], spec2: Specification[T])
    extends AbstractSpecification[T]:
  override def isSatisfiedBy(t: T): Boolean =
    spec1.isSatisfiedBy(t) && spec2.isSatisfiedBy(t)
