package se.citerus.dddsample.domain.model.cargo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TrackingIdTest extends AnyFunSuite with Matchers:

  test("constructor rejects null id") {
    a[NullPointerException] should be thrownBy TrackingId(null.asInstanceOf[String])
  }

  test("constructor rejects empty id") {
    an[IllegalArgumentException] should be thrownBy TrackingId("")
  }

  test("idString round-trips the input") {
    TrackingId("XYZ123").idString shouldEqual "XYZ123"
  }

  test("sameValueAs is reflexive and equals-aware") {
    TrackingId("ABC").sameValueAs(TrackingId("ABC")) shouldBe true
    TrackingId("ABC").sameValueAs(TrackingId("DEF")) shouldBe false
  }
