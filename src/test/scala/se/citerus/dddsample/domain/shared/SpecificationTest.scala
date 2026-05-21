package se.citerus.dddsample.domain.shared

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Combined translation of upstream `AndSpecificationTest`,
 * `OrSpecificationTest`, and `NotSpecificationTest` (and the
 * `AlwaysTrueSpec` / `AlwaysFalseSpec` fixtures).
 */
class SpecificationTest extends AnyFunSuite with Matchers:

  // Fixtures matching upstream AlwaysTrueSpec / AlwaysFalseSpec.
  private val t = new AbstractSpecification[Any]:
    def isSatisfiedBy(x: Any): Boolean = true
  private val f = new AbstractSpecification[Any]:
    def isSatisfiedBy(x: Any): Boolean = false

  private val any: Any = new Object()

  // --- AND ---------------------------------------------------------------
  test("AND: true && true == true") {
    new AndSpecification[Any](t, t).isSatisfiedBy(any) shouldBe true
  }
  test("AND: false && true == false") {
    new AndSpecification[Any](f, t).isSatisfiedBy(any) shouldBe false
  }
  test("AND: true && false == false") {
    new AndSpecification[Any](t, f).isSatisfiedBy(any) shouldBe false
  }
  test("AND: false && false == false") {
    new AndSpecification[Any](f, f).isSatisfiedBy(any) shouldBe false
  }

  // --- OR ----------------------------------------------------------------
  test("OR: true || true == true") {
    new OrSpecification[Any](t, t).isSatisfiedBy(any) shouldBe true
  }
  test("OR: false || true == true") {
    new OrSpecification[Any](f, t).isSatisfiedBy(any) shouldBe true
  }
  test("OR: true || false == true") {
    new OrSpecification[Any](t, f).isSatisfiedBy(any) shouldBe true
  }
  test("OR: false || false == false") {
    new OrSpecification[Any](f, f).isSatisfiedBy(any) shouldBe false
  }

  // --- NOT ---------------------------------------------------------------
  test("NOT: !true == false") {
    new NotSpecification[Any](t).isSatisfiedBy(any) shouldBe false
  }
  test("NOT: !false == true") {
    new NotSpecification[Any](f).isSatisfiedBy(any) shouldBe true
  }

  // --- Composition via the trait methods --------------------------------
  test("composition: t.and(t).or(f) == true") {
    t.and(t).or(f).isSatisfiedBy(any) shouldBe true
  }
  test("composition: t.not(f).and(t) == true") {
    t.not(f).and(t).isSatisfiedBy(any) shouldBe true
  }
