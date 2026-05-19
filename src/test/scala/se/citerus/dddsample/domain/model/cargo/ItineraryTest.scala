package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.handling.{HandlingEvent, HandlingEventType}
import se.citerus.dddsample.domain.model.location.{Location, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{CarrierMovement, Schedule, Voyage, VoyageNumber}

class ItineraryTest extends AnyFunSuite with Matchers:

  private val SHANGHAI  = Location(UnLocode("CNSHA"), "Shanghai")
  private val ROTTERDAM = Location(UnLocode("NLRTM"), "Rotterdam")
  private val GOTHENBURG = Location(UnLocode("SEGOT"), "Gothenburg")
  private val HANGZHOU  = Location(UnLocode("CNHGH"), "Hangzhou")
  private val HELSINKI  = Location(UnLocode("FIHEL"), "Helsinki")
  private val STOCKHOLM = Location(UnLocode("SESTO"), "Stockholm")

  private val voyage = new Voyage(
    VoyageNumber("0123"),
    Schedule(List(
      CarrierMovement(SHANGHAI,  ROTTERDAM,  Instant.ofEpochMilli(1), Instant.ofEpochMilli(2)),
      CarrierMovement(ROTTERDAM, GOTHENBURG, Instant.ofEpochMilli(3), Instant.ofEpochMilli(4))
    ))
  )
  private val wrongVoyage = new Voyage(
    VoyageNumber("666"),
    Schedule(List(CarrierMovement(STOCKHOLM, HELSINKI, Instant.ofEpochMilli(1), Instant.ofEpochMilli(2))))
  )

  private val routeSpec = RouteSpecification(SHANGHAI, GOTHENBURG, Instant.ofEpochMilli(Long.MaxValue / 2))
  private val cargo     = new Cargo(TrackingId("CARGO1"), routeSpec)

  private val itinerary = Itinerary(List(
    Leg(voyage, SHANGHAI,  ROTTERDAM,  Instant.ofEpochMilli(10), Instant.ofEpochMilli(20)),
    Leg(voyage, ROTTERDAM, GOTHENBURG, Instant.ofEpochMilli(30), Instant.ofEpochMilli(40))
  ))

  test("rejects null and empty leg lists") {
    a[NullPointerException]    should be thrownBy Itinerary(null)
    an[IllegalArgumentException] should be thrownBy Itinerary(Nil)
  }

  test("on-track happy-path events are expected") {
    val now = Instant.ofEpochMilli(50)
    val events = List(
      HandlingEvent(cargo, now, now, HandlingEventType.RECEIVE, SHANGHAI),
      HandlingEvent(cargo, now, now, HandlingEventType.LOAD,    SHANGHAI,  voyage),
      HandlingEvent(cargo, now, now, HandlingEventType.UNLOAD,  ROTTERDAM, voyage),
      HandlingEvent(cargo, now, now, HandlingEventType.LOAD,    ROTTERDAM, voyage),
      HandlingEvent(cargo, now, now, HandlingEventType.UNLOAD,  GOTHENBURG, voyage),
      HandlingEvent(cargo, now, now, HandlingEventType.CLAIM,   GOTHENBURG),
      HandlingEvent(cargo, now, now, HandlingEventType.CUSTOMS, GOTHENBURG)
    )
    events.foreach { e =>
      withClue(s"expected $e to be on track: ") {
        itinerary.isExpected(e) shouldBe true
      }
    }
  }

  test("RECEIVE at wrong location is unexpected") {
    val e = HandlingEvent(cargo, Instant.ofEpochMilli(50), Instant.ofEpochMilli(50),
                          HandlingEventType.RECEIVE, HANGZHOU)
    itinerary.isExpected(e) shouldBe false
  }

  test("LOAD onto the wrong voyage at a correct location is unexpected") {
    val e = HandlingEvent(cargo, Instant.ofEpochMilli(50), Instant.ofEpochMilli(50),
                          HandlingEventType.LOAD, ROTTERDAM, wrongVoyage)
    itinerary.isExpected(e) shouldBe false
  }

  test("CLAIM somewhere other than final destination is unexpected") {
    val e = HandlingEvent(cargo, Instant.ofEpochMilli(50), Instant.ofEpochMilli(50),
                          HandlingEventType.CLAIM, ROTTERDAM)
    itinerary.isExpected(e) shouldBe false
  }

  test("initialDepartureLocation, finalArrivalLocation, finalArrivalDate") {
    itinerary.initialDepartureLocation shouldEqual SHANGHAI
    itinerary.finalArrivalLocation     shouldEqual GOTHENBURG
    itinerary.finalArrivalDate         shouldEqual Instant.ofEpochMilli(40)
  }

  test("EMPTY accepts everything") {
    val e = HandlingEvent(cargo, Instant.ofEpochMilli(50), Instant.ofEpochMilli(50),
                          HandlingEventType.RECEIVE, HANGZHOU)
    Itinerary.EMPTY.isExpected(e) shouldBe true
    Itinerary.EMPTY.initialDepartureLocation shouldEqual Location.UNKNOWN
    Itinerary.EMPTY.finalArrivalLocation     shouldEqual Location.UNKNOWN
  }
