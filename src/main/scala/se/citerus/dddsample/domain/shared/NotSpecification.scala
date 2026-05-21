package se.citerus.dddsample.domain.shared

/** NOT decorator: inverts the operand. */
final class NotSpecification[T](spec: Specification[T]) extends AbstractSpecification[T]:
  override def isSatisfiedBy(t: T): Boolean = !spec.isSatisfiedBy(t)
