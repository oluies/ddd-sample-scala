package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.handling.{
  HandlingEvent,
  HandlingEventType,
  HandlingHistory
}
import se.citerus.dddsample.domain.model.location.{Location, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{CarrierMovement, Schedule, Voyage, VoyageNumber}

class CargoTest extends AnyFunSuite with Matchers:

  private val SHA = Location(UnLocode("CNSHA"), "Shanghai")
  private val RTM = Location(UnLocode("NLRTM"), "Rotterdam")
  private val GOT = Location(UnLocode("SEGOT"), "Gothenburg")

  private val deadline = Instant.parse("2026-12-01T00:00:00Z")
  private val voyage = new Voyage(
    VoyageNumber("V1"),
    Schedule(
      List(
        CarrierMovement(SHA, RTM, Instant.ofEpochMilli(1), Instant.ofEpochMilli(2)),
        CarrierMovement(RTM, GOT, Instant.ofEpochMilli(3), Instant.ofEpochMilli(4))
      )
    )
  )

  private def itinerary(arrival: Instant) = Itinerary(
    List(
      Leg(voyage, SHA, RTM, Instant.ofEpochMilli(10), Instant.ofEpochMilli(20)),
      Leg(voyage, RTM, GOT, Instant.ofEpochMilli(30), arrival)
    )
  )

  test("origin is fixed at the initial route spec's origin and survives respecify") {
    val cargo = Cargo(TrackingId("ABC"), RouteSpecification(SHA, GOT, deadline))
    cargo.origin shouldEqual SHA
    val rerouted = cargo.specifyNewRoute(RouteSpecification(RTM, GOT, deadline))
    rerouted.origin shouldEqual SHA
  }

  test("freshly created cargo is NOT_ROUTED with NOT_RECEIVED transport status") {
    val cargo = Cargo(TrackingId("ABC"), RouteSpecification(SHA, GOT, deadline))
    cargo.delivery.routingStatus shouldEqual RoutingStatus.NOT_ROUTED
    cargo.delivery.transportStatus shouldEqual TransportStatus.NOT_RECEIVED
  }

  test("assignToRoute returns a new Cargo with delivery recomputed to ROUTED") {
    val cargo  = Cargo(TrackingId("ABC"), RouteSpecification(SHA, GOT, deadline))
    val routed = cargo.assignToRoute(itinerary(Instant.ofEpochMilli(40)))
    routed.delivery.routingStatus shouldEqual RoutingStatus.ROUTED
    // Original is unchanged — confirms immutability.
    cargo.delivery.routingStatus shouldEqual RoutingStatus.NOT_ROUTED
  }

  test("identity equality is by tracking id") {
    val spec = RouteSpecification(SHA, GOT, deadline)
    val a    = Cargo(TrackingId("ABC"), spec)
    val b    = Cargo(TrackingId("ABC"), spec)
    val c    = Cargo(TrackingId("DEF"), spec)
    a shouldEqual b
    a.sameIdentityAs(b) shouldBe true
    (a == c) shouldBe false
  }

  test("deriveDeliveryProgress with a LOAD event reports ONBOARD_CARRIER") {
    val routed = Cargo(TrackingId("ABC"), RouteSpecification(SHA, GOT, deadline))
      .assignToRoute(itinerary(Instant.ofEpochMilli(40)))
    val loaded = HandlingEvent(
      routed,
      Instant.ofEpochMilli(15),
      Instant.ofEpochMilli(15),
      HandlingEventType.LOAD,
      SHA,
      voyage
    )
    val updated = routed.deriveDeliveryProgress(HandlingHistory(List(loaded)))
    updated.delivery.transportStatus shouldEqual TransportStatus.ONBOARD_CARRIER
    updated.delivery.currentVoyage shouldEqual voyage
  }
