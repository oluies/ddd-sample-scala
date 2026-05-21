package se.citerus.dddsample.domain.model.voyage

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.location.{Location, UnLocode}

class VoyageTest extends AnyFunSuite with Matchers:

  private val STOCKHOLM = Location(UnLocode("SESTO"), "Stockholm")
  private val HAMBURG   = Location(UnLocode("DEHAM"), "Hamburg")
  private val NEWYORK   = Location(UnLocode("USNYC"), "New York")

  private val t0 = Instant.ofEpochSecond(0)
  private val t1 = Instant.ofEpochSecond(3600)
  private val t2 = Instant.ofEpochSecond(7200)
  private val t3 = Instant.ofEpochSecond(10800)

  test("identity is by VoyageNumber") {
    val v1 =
      new Voyage(VoyageNumber("V001"), Schedule(List(CarrierMovement(STOCKHOLM, HAMBURG, t0, t1))))
    val v2 =
      new Voyage(VoyageNumber("V001"), Schedule(List(CarrierMovement(HAMBURG, NEWYORK, t2, t3))))
    val v3 =
      new Voyage(VoyageNumber("V002"), Schedule(List(CarrierMovement(STOCKHOLM, HAMBURG, t0, t1))))

    v1.sameIdentityAs(v2) shouldBe true // same voyage number, different schedule
    v1.sameIdentityAs(v3) shouldBe false
    (v1 == v2) shouldBe true
    (v1 == v3) shouldBe false
  }

  test("rejects null schedule") {
    a[NullPointerException] should be thrownBy new Voyage(VoyageNumber("V001"), null)
  }
  // Note: `Voyage(null, ...)` is rejected at compile time because
  // VoyageNumber is an opaque type. Stronger than the upstream Java
  // runtime check.

  test("Builder accumulates carrier movements with linked departure/arrival") {
    val voyage = new Voyage.Builder(VoyageNumber("V001"), STOCKHOLM)
      .addMovement(HAMBURG, t0, t1)
      .addMovement(NEWYORK, t2, t3)
      .build()

    voyage.voyageNumber.idString shouldEqual "V001"
    val movements = voyage.schedule.carrierMovements
    movements.size shouldEqual 2
    movements(0) shouldEqual CarrierMovement(STOCKHOLM, HAMBURG, t0, t1)
    movements(1) shouldEqual CarrierMovement(HAMBURG, NEWYORK, t2, t3)
  }

  test("Builder rejects null Location") {
    a[NullPointerException] should be thrownBy new Voyage.Builder(VoyageNumber("V001"), null)
  }
