package se.citerus.dddsample.domain.model.handling

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.cargo.{Cargo, RouteSpecification, TrackingId}
import se.citerus.dddsample.domain.model.location.{Location, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{CarrierMovement, Schedule, Voyage, VoyageNumber}

class HandlingHistoryTest extends AnyFunSuite with Matchers:

  private val SHA = Location(UnLocode("CNSHA"), "Shanghai")
  private val DAL = Location(UnLocode("USDAL"), "Dallas")
  private val GOT = Location(UnLocode("SEGOT"), "Gothenburg")
  private val voyage = new Voyage(
    VoyageNumber("V1"),
    Schedule(List(CarrierMovement(SHA, DAL, Instant.ofEpochMilli(1), Instant.ofEpochMilli(2))))
  )
  private val cargo = new Cargo(
    TrackingId("ABC"),
    RouteSpecification(SHA, GOT, Instant.parse("2026-12-01T00:00:00Z"))
  )

  private val t1 = Instant.parse("2026-03-05T00:00:00Z")
  private val t2 = Instant.parse("2026-03-10T00:00:00Z")

  private val event1 = HandlingEvent(cargo, t1, Instant.ofEpochMilli(100), HandlingEventType.LOAD,   SHA, voyage)
  // Same business identity (same cargo, voyage, location, type, completionTime) — different registrationTime.
  private val event1dup = HandlingEvent(cargo, t1, Instant.ofEpochMilli(200), HandlingEventType.LOAD,   SHA, voyage)
  private val event2 = HandlingEvent(cargo, t2, Instant.ofEpochMilli(150), HandlingEventType.UNLOAD, DAL, voyage)

  test("EMPTY has no events and no most-recent event") {
    HandlingHistory.EMPTY.distinctEventsByCompletionTime shouldBe empty
    HandlingHistory.EMPTY.mostRecentlyCompletedEvent     shouldBe None
  }

  test("distinctEventsByCompletionTime deduplicates and orders ascending") {
    val history = HandlingHistory(List(event2, event1, event1dup))
    val distinct = history.distinctEventsByCompletionTime
    distinct should have size 2
    distinct.map(_.completionTime) shouldEqual List(t1, t2)
  }

  test("mostRecentlyCompletedEvent returns the latest by completion time") {
    HandlingHistory(List(event1, event2, event1dup)).mostRecentlyCompletedEvent shouldEqual Some(event2)
  }

  test("rejects null events collection") {
    a[NullPointerException] should be thrownBy HandlingHistory(null)
  }
