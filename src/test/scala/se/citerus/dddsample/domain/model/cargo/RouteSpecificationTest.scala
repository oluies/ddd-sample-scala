package se.citerus.dddsample.domain.model.cargo;


import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.EasyMockSugar;

import se.citerus.dddsample.application.util.DateTestUtil
import se.citerus.dddsample.domain.model.cargo._;
import se.citerus.dddsample.domain.model.handling._;
import se.citerus.dddsample.domain.model.location._;
import se.citerus.dddsample.domain.model.location.SampleLocations._;
import se.citerus.dddsample.domain.model.voyage._;
import se.citerus.dddsample.domain.model.voyage.SampleVoyages._;

import java.util.Date;

class RouteSpecificationTest extends TestCase with AssertionsForJUnit with EasyMockSugar with DateTestUtil {

  val hongKongTokyoNewYork = new VoyageBuilder(
    new VoyageNumber("V001"), HONGKONG).
    addMovement(TOKYO, toDate("2009-02-01"), toDate("2009-02-05")).
    addMovement(NEWYORK, toDate("2009-02-06"), toDate("2009-02-10")).
    addMovement(HONGKONG, toDate("2009-02-11"), toDate("2009-02-14")).
    build();

  val dallasNewYorkChicago = new VoyageBuilder(
    new VoyageNumber("V002"), DALLAS).
    addMovement(NEWYORK, toDate("2009-02-06"), toDate("2009-02-07")).
    addMovement(CHICAGO, toDate("2009-02-12"), toDate("2009-02-20")).
    build();

  // TODO:
  // it shouldn't be possible to create Legs that have load/unload locations
  // and/or dates that don't match the voyage's carrier movements.
  val itinerary = Itinerary(List(
      new Leg(hongKongTokyoNewYork, HONGKONG, NEWYORK,
              toDate("2009-02-01"), toDate("2009-02-10")),
      new Leg(dallasNewYorkChicago, NEWYORK, CHICAGO,
              toDate("2009-02-12"), toDate("2009-02-20")))
  );

  def testIsSatisfiedBy_Success() = {
    val routeSpecification = new RouteSpecification(
      HONGKONG, CHICAGO, toDate("2009-03-01")
    );

    assertTrue(routeSpecification.isSatisfiedBy(itinerary));
  }

  def testIsSatisfiedBy_WrongOrigin() = {
    val routeSpecification = new RouteSpecification(
      HANGZHOU, CHICAGO, toDate("2009-03-01")
    );

    assertFalse(routeSpecification.isSatisfiedBy(itinerary));
  }

  def testIsSatisfiedBy_WrongDestination() = {
    val routeSpecification = new RouteSpecification(
      HONGKONG, DALLAS, toDate("2009-03-01")
    );

    assertFalse(routeSpecification.isSatisfiedBy(itinerary));
  }

  def testIsSatisfiedBy_MissedDeadline() = {
    val routeSpecification = new RouteSpecification(
      HONGKONG, CHICAGO, toDate("2009-02-15")
    );

    assertFalse(routeSpecification.isSatisfiedBy(itinerary));
  }

}
