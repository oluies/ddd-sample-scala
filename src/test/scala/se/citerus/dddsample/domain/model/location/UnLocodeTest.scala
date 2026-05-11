package se.citerus.dddsample.domain.model.location

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

/** Translation of upstream `UnLocodeTest`. */
class UnLocodeTest extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  test("accepts valid UN/LOCODEs") {
    for input <- List("AA234", "AAA9B", "AAAAA") do
      noException should be thrownBy UnLocode(input)
  }

  test("rejects invalid UN/LOCODEs") {
    for input <- List("AAAA", "AAAAAA", "22AAA", "AA111", "") do
      an[IllegalArgumentException] should be thrownBy UnLocode(input)
  }

  test("rejects null") {
    an[IllegalArgumentException] should be thrownBy UnLocode(null)
  }

  test("idString uppercases the input") {
    UnLocode("AbcDe").idString shouldEqual "ABCDE"
  }

  test("equality is case-insensitive (because input is uppercased)") {
    val allCaps   = UnLocode("ABCDE")
    val mixedCase = UnLocode("aBcDe")

    allCaps    shouldEqual mixedCase
    mixedCase  shouldEqual allCaps
    allCaps    shouldEqual allCaps
    allCaps    should not equal UnLocode("FGHIJ")
  }

  test("hashCode agrees with equals") {
    val allCaps   = UnLocode("ABCDE")
    val mixedCase = UnLocode("aBcDe")
    allCaps.hashCode shouldEqual mixedCase.hashCode
  }

  // --- Property tests (Scala 3 idiom: ScalaCheck for value-object invariants)
  private val validInput: Gen[String] =
    for
      cc <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
      lc <- Gen.listOfN(3, Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('2' to '9'))).map(_.mkString)
    yield cc + lc

  test("any valid input round-trips through idString as upper-case") {
    forAll(validInput) { s =>
      UnLocode(s).idString shouldEqual s.toUpperCase
    }
  }

  test("any valid input equals itself by sameValueAs") {
    forAll(validInput) { s =>
      val u = UnLocode(s)
      u.sameValueAs(u) shouldBe true
    }
  }
