package se.citerus.dddsample.domain.model.cargo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.application.util.DateTestUtil
import se.citerus.dddsample.domain.model.location.SampleLocations.*
import se.citerus.dddsample.domain.model.voyage.{VoyageBuilder, VoyageNumber}

class RouteSpecificationTest extends AnyFunSuite with Matchers with DateTestUtil {

  private val hongKongTokyoNewYork = new VoyageBuilder(new VoyageNumber("V001"), HONGKONG)
    .addMovement(TOKYO, toDate("2009-02-01"), toDate("2009-02-05"))
    .addMovement(NEWYORK, toDate("2009-02-06"), toDate("2009-02-10"))
    .addMovement(HONGKONG, toDate("2009-02-11"), toDate("2009-02-14"))
    .build()

  private val dallasNewYorkChicago = new VoyageBuilder(new VoyageNumber("V002"), DALLAS)
    .addMovement(NEWYORK, toDate("2009-02-06"), toDate("2009-02-07"))
    .addMovement(CHICAGO, toDate("2009-02-12"), toDate("2009-02-20"))
    .build()

  // TODO: it shouldn't be possible to create Legs that have load/unload
  // locations and/or dates that don't match the voyage's carrier movements.
  private val itinerary = Itinerary(
    List(
      new Leg(hongKongTokyoNewYork, HONGKONG, NEWYORK, toDate("2009-02-01"), toDate("2009-02-10")),
      new Leg(dallasNewYorkChicago, NEWYORK, CHICAGO, toDate("2009-02-12"), toDate("2009-02-20"))
    )
  )

  test("isSatisfiedBy: success") {
    val routeSpecification = new RouteSpecification(HONGKONG, CHICAGO, toDate("2009-03-01"))
    routeSpecification.isSatisfiedBy(itinerary) shouldBe true
  }

  test("isSatisfiedBy: wrong origin") {
    val routeSpecification = new RouteSpecification(HANGZHOU, CHICAGO, toDate("2009-03-01"))
    routeSpecification.isSatisfiedBy(itinerary) shouldBe false
  }

  test("isSatisfiedBy: wrong destination") {
    val routeSpecification = new RouteSpecification(HONGKONG, DALLAS, toDate("2009-03-01"))
    routeSpecification.isSatisfiedBy(itinerary) shouldBe false
  }

  test("isSatisfiedBy: missed deadline") {
    val routeSpecification = new RouteSpecification(HONGKONG, CHICAGO, toDate("2009-02-15"))
    routeSpecification.isSatisfiedBy(itinerary) shouldBe false
  }

}
