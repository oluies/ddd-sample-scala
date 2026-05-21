package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.location.{Location, UnLocode}

class RouteSpecificationTest extends AnyFunSuite with Matchers:

  private val HONGKONG = Location(UnLocode("HKHKG"), "Hong Kong")
  private val CHICAGO  = Location(UnLocode("USCHI"), "Chicago")
  private val deadline = Instant.parse("2026-06-01T00:00:00Z")

  test("constructor rejects null arguments") {
    a[NullPointerException] should be thrownBy
      RouteSpecification(null, CHICAGO, deadline)
    a[NullPointerException] should be thrownBy
      RouteSpecification(HONGKONG, null, deadline)
    a[NullPointerException] should be thrownBy
      RouteSpecification(HONGKONG, CHICAGO, null)
  }

  test("constructor rejects same origin and destination") {
    an[IllegalArgumentException] should be thrownBy
      RouteSpecification(HONGKONG, HONGKONG, deadline)
  }

  test("isSatisfiedBy rejects a null itinerary") {
    RouteSpecification(HONGKONG, CHICAGO, deadline).isSatisfiedBy(null) shouldBe false
  }

  test("equality is by-value") {
    val a = RouteSpecification(HONGKONG, CHICAGO, deadline)
    val b = RouteSpecification(HONGKONG, CHICAGO, deadline)
    a shouldEqual b
    a.sameValueAs(b) shouldBe true
  }
