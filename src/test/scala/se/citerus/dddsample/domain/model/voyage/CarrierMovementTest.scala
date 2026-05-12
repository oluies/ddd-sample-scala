package se.citerus.dddsample.domain.model.voyage

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.location.{Location, UnLocode}

/** Translation of upstream `CarrierMovementTest`. Defines the two test
  * locations inline rather than depending on `SampleLocations`, which lives
  * in `infrastructure.sampledata` (phase 10, not yet ported).
  */
class CarrierMovementTest extends AnyFunSuite with Matchers:

  private val STOCKHOLM = Location(UnLocode("SESTO"), "Stockholm")
  private val HAMBURG   = Location(UnLocode("DEHAM"), "Hamburg")

  test("constructor rejects null arguments") {
    a[NullPointerException] should be thrownBy
      CarrierMovement(STOCKHOLM, null, Instant.now(), Instant.now())
    a[NullPointerException] should be thrownBy
      CarrierMovement(STOCKHOLM, HAMBURG, null, Instant.now())
    // Legal
    noException should be thrownBy
      CarrierMovement(STOCKHOLM, HAMBURG, Instant.now(), Instant.now())
  }

  test("sameValueAs, equals, and hashCode agree on attribute equality") {
    val t = Instant.ofEpochMilli(System.currentTimeMillis())
    // Same instant in distinct objects (production carrier movements
    // typically arrive from different processes so the Instant references
    // differ even when their values are equal).
    val cm1 = CarrierMovement(STOCKHOLM, HAMBURG, Instant.ofEpochMilli(t.toEpochMilli), Instant.ofEpochMilli(t.toEpochMilli))
    val cm2 = CarrierMovement(STOCKHOLM, HAMBURG, Instant.ofEpochMilli(t.toEpochMilli), Instant.ofEpochMilli(t.toEpochMilli))
    val cm3 = CarrierMovement(HAMBURG, STOCKHOLM, Instant.ofEpochMilli(t.toEpochMilli), Instant.ofEpochMilli(t.toEpochMilli))
    val cm4 = CarrierMovement(HAMBURG, STOCKHOLM, Instant.ofEpochMilli(t.toEpochMilli), Instant.ofEpochMilli(t.toEpochMilli))

    cm1.sameValueAs(cm2) shouldBe true
    cm2.sameValueAs(cm3) shouldBe false
    cm3.sameValueAs(cm4) shouldBe true

    (cm1 == cm2) shouldBe true
    (cm2 == cm3) shouldBe false
    (cm3 == cm4) shouldBe true

    (cm1.hashCode == cm2.hashCode) shouldBe true
    (cm2.hashCode == cm3.hashCode) shouldBe false
    (cm3.hashCode == cm4.hashCode) shouldBe true
  }

  test("NONE is the null object") {
    CarrierMovement.NONE.departureLocation shouldEqual Location.UNKNOWN
    CarrierMovement.NONE.arrivalLocation   shouldEqual Location.UNKNOWN
  }
