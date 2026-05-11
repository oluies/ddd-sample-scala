package se.citerus.dddsample.domain.model.location

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** Translation of upstream `LocationTest`. */
class LocationTest extends AnyFunSuite with Matchers:

  test("equality is by UN locode") {
    Location(UnLocode("ATEST"), "test-name")  shouldEqual Location(UnLocode("ATEST"), "test-name")
    Location(UnLocode("ATEST"), "test-name")  should not equal Location(UnLocode("TESTB"), "test-name")
  }

  test("equal to itself") {
    val l = Location(UnLocode("ATEST"), "test-name")
    l shouldEqual l
  }

  test("never equal to null") {
    val l = Location(UnLocode("ATEST"), "test-name")
    l should not equal null
  }

  test("UNKNOWN equals itself") {
    Location.UNKNOWN shouldEqual Location.UNKNOWN
  }

  test("rejects null name") {
    a[NullPointerException] should be thrownBy Location(UnLocode("ATEST"), null)
  }
  // Note: passing `null` for the UnLocode parameter is rejected at compile
  // time by Scala 3's type system (UnLocode is an opaque type, not nullable).
  // The upstream Java test covers that case at runtime; here we get a
  // stronger guarantee from the compiler.

  test("toString includes name and UN locode") {
    Location(UnLocode("SESTO"), "Stockholm").toString shouldEqual "Stockholm [SESTO]"
  }
