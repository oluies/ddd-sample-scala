package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.location.{Location, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{Schedule, CarrierMovement, Voyage, VoyageNumber}

class LegTest extends AnyFunSuite with Matchers:

  private val SES = Location(UnLocode("SESTO"), "Stockholm")
  private val DEH = Location(UnLocode("DEHAM"), "Hamburg")
  private val voyage = new Voyage(
    VoyageNumber("V01"),
    Schedule(List(CarrierMovement(SES, DEH, Instant.ofEpochMilli(0), Instant.ofEpochMilli(1))))
  )

  test("constructor rejects null components") {
    a[NullPointerException] should be thrownBy
      Leg(null, SES, DEH, Instant.now(), Instant.now())
    a[NullPointerException] should be thrownBy
      Leg(voyage, SES, DEH, null, Instant.now())
  }

  test("two legs with same fields are sameValueAs and equal") {
    val t1 = Instant.ofEpochMilli(100)
    val t2 = Instant.ofEpochMilli(200)
    val a  = Leg(voyage, SES, DEH, t1, t2)
    val b  = Leg(voyage, SES, DEH, t1, t2)
    a.sameValueAs(b) shouldBe true
    a shouldEqual b
    a.hashCode shouldEqual b.hashCode
  }
