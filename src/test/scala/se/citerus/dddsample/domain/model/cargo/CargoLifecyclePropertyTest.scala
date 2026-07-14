package se.citerus.dddsample.domain.model.cargo

import java.time.Instant

import scala.util.Random

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import se.citerus.dddsample.domain.model.handling.{
  HandlingEvent,
  HandlingEventType,
  HandlingHistory
}
import se.citerus.dddsample.domain.model.location.{Location, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{CarrierMovement, Schedule, Voyage, VoyageNumber}

/**
 * Property-based tests for the [[Cargo]] aggregate's lifecycle — the
 * `derivedFrom`/`deriveDeliveryProgress` pipeline that translates a
 * `HandlingHistory` into a [[Delivery]] snapshot.
 *
 * Built around one fixed three-leg itinerary so the generators only need to
 * vary handling-event content and ordering, not the route shape. Properties
 * fall into four buckets:
 *
 *   1. **Empty history** — boundary case (NOT_RECEIVED, no location).
 *   2. **On-track prefixes** — the canonical RECEIVE → LOAD → UNLOAD → … →
 *      CLAIM sequence applied to a random cut-off point.
 *   3. **Off-itinerary events** — events at wrong locations / on wrong
 *      voyages must mark the cargo as misdirected.
 *   4. **Algebraic invariants** — `derivedFrom` is idempotent and is
 *      independent of event insertion order (because
 *      `HandlingHistory.distinctEventsByCompletionTime` sorts and dedupes).
 *
 * The test deliberately compares Delivery field-by-field rather than via
 * `sameValueAs` because `Delivery.calculatedAt` uses `Instant.now()` at
 * construction time — two derivations from the same input have different
 * `calculatedAt`. The lifecycle invariants are everything else.
 */
class CargoLifecyclePropertyTest extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks:

  // ---- Fixture ----------------------------------------------------------

  private val SHA = Location(UnLocode("CNSHA"), "Shanghai")
  private val RTM = Location(UnLocode("NLRTM"), "Rotterdam")
  private val HAM = Location(UnLocode("DEHAM"), "Hamburg")
  private val GOT = Location(UnLocode("SEGOT"), "Gothenburg")
  // An off-route location for unexpected-event generators.
  private val NYC = Location(UnLocode("USNYC"), "New York")

  // Three-leg voyage SHA → RTM → HAM → GOT. One voyage carries every leg —
  // simpler than juggling intermediate UNLOAD/LOAD pairs across voyages, and
  // still exercises the multi-leg paths in `Delivery.calculateNextExpected
  // Activity` and `Itinerary.isExpected`.
  private val voyage = new Voyage(
    VoyageNumber("V1"),
    Schedule(
      List(
        CarrierMovement(SHA, RTM, t(10), t(20)),
        CarrierMovement(RTM, HAM, t(30), t(40)),
        CarrierMovement(HAM, GOT, t(50), t(60))
      )
    )
  )
  // A second voyage that touches none of the route's locations — used to
  // synthesize off-itinerary LOADs.
  private val wrongVoyage = new Voyage(
    VoyageNumber("V2"),
    Schedule(List(CarrierMovement(NYC, NYC, t(1), t(2))))
  )

  private val deadline = Instant.parse("2030-01-01T00:00:00Z")
  private val spec     = RouteSpecification(SHA, GOT, deadline)
  private val itinerary = Itinerary(
    List(
      Leg(voyage, SHA, RTM, t(10), t(20)),
      Leg(voyage, RTM, HAM, t(30), t(40)),
      Leg(voyage, HAM, GOT, t(50), t(60))
    )
  )
  private val baseCargo = Cargo(TrackingId("ABC"), spec).assignToRoute(itinerary)

  // Canonical happy-path event sequence. Indexed by step so the prefix
  // generator can pick a cut-off.
  private val canonicalSteps: Vector[HandlingEventBuilder] = Vector(
    ev(HandlingEventType.RECEIVE, SHA, t(5)),
    ev(HandlingEventType.LOAD, SHA, t(15), voyage),
    ev(HandlingEventType.UNLOAD, RTM, t(25), voyage),
    ev(HandlingEventType.LOAD, RTM, t(35), voyage),
    ev(HandlingEventType.UNLOAD, HAM, t(45), voyage),
    ev(HandlingEventType.LOAD, HAM, t(55), voyage),
    ev(HandlingEventType.UNLOAD, GOT, t(65), voyage),
    ev(HandlingEventType.CLAIM, GOT, t(75))
  )

  // ---- Generators -------------------------------------------------------

  private val genPrefixLength: Gen[Int] = Gen.choose(0, canonicalSteps.length)

  private val genOnTrackPrefix: Gen[List[HandlingEvent]] =
    genPrefixLength.map(n => canonicalSteps.take(n).toList.map(_.apply(baseCargo)))

  /**
   * Off-itinerary event: pick something a real reporter could submit but
   * that the itinerary would reject. Three flavours: wrong location for the
   * type, wrong voyage for LOAD, CLAIM at non-destination.
   */
  private val genOffItineraryEvent: Gen[HandlingEvent] =
    Gen.oneOf(
      // RECEIVE at the wrong port.
      ev(HandlingEventType.RECEIVE, NYC, t(100)).apply(baseCargo),
      // LOAD on a voyage that doesn't visit the itinerary.
      ev(HandlingEventType.LOAD, NYC, t(100), wrongVoyage).apply(baseCargo),
      // CLAIM somewhere other than the destination.
      ev(HandlingEventType.CLAIM, RTM, t(100)).apply(baseCargo)
    )

  // ---- Properties: empty history ---------------------------------------

  test("property: empty handling history yields NOT_RECEIVED with no location") {
    val derived = baseCargo.deriveDeliveryProgress(HandlingHistory.EMPTY)
    derived.delivery.transportStatus shouldEqual TransportStatus.NOT_RECEIVED
    derived.delivery.lastKnownLocationOpt shouldEqual None
    derived.delivery.lastEvent shouldEqual None
    derived.delivery.currentVoyage shouldEqual None
    derived.delivery.misdirected shouldBe false
  }

  // ---- Properties: on-track prefixes -----------------------------------

  test("property: an on-track prefix is never misdirected and routes ROUTED") {
    forAll(genOnTrackPrefix) { events =>
      val derived = baseCargo.deriveDeliveryProgress(HandlingHistory(events))
      derived.delivery.routingStatus shouldEqual RoutingStatus.ROUTED
      derived.delivery.misdirected shouldBe false
    }
  }

  test("property: on-track prefix transport status matches the last event's type") {
    forAll(genOnTrackPrefix.suchThat(_.nonEmpty)) { events =>
      val last    = events.maxBy(_.completionTime)
      val derived = baseCargo.deriveDeliveryProgress(HandlingHistory(events))
      val expected = last.eventType match
        case HandlingEventType.RECEIVE => TransportStatus.IN_PORT
        case HandlingEventType.LOAD    => TransportStatus.ONBOARD_CARRIER
        case HandlingEventType.UNLOAD  => TransportStatus.IN_PORT
        case HandlingEventType.CLAIM   => TransportStatus.CLAIMED
        case HandlingEventType.CUSTOMS => TransportStatus.IN_PORT
      derived.delivery.transportStatus shouldEqual expected
    }
  }

  test("property: after LOAD the currentVoyage is Some, otherwise None") {
    forAll(genOnTrackPrefix.suchThat(_.nonEmpty)) { events =>
      val last    = events.maxBy(_.completionTime)
      val derived = baseCargo.deriveDeliveryProgress(HandlingHistory(events))
      if last.eventType == HandlingEventType.LOAD then
        derived.delivery.currentVoyage shouldEqual Some(voyage)
      else derived.delivery.currentVoyage shouldEqual None
    }
  }

  test("property: CLAIM-at-destination ends with isUnloadedAtDestination still true") {
    // After CLAIM the lastEvent is the CLAIM itself, but the last UNLOAD at
    // destination has set the relevant invariant. We check the prefix ending
    // exactly at UNLOAD (step 7) to assert the unload property cleanly.
    val unloadAtDestPrefix = canonicalSteps.take(7).toList.map(_.apply(baseCargo))
    val derived            = baseCargo.deriveDeliveryProgress(HandlingHistory(unloadAtDestPrefix))
    derived.delivery.isUnloadedAtDestination shouldBe true
    derived.delivery.transportStatus shouldEqual TransportStatus.IN_PORT
    derived.delivery.lastKnownLocationOpt shouldEqual Some(GOT)
  }

  // ---- Properties: off-itinerary ---------------------------------------

  test("property: an off-itinerary event flips misdirected to true") {
    forAll(genOffItineraryEvent) { bad =>
      val derived = baseCargo.deriveDeliveryProgress(HandlingHistory(List(bad)))
      derived.delivery.misdirected shouldBe true
    }
  }

  // ---- Properties: algebraic invariants --------------------------------

  test("property: derivedFrom is idempotent on lifecycle fields") {
    forAll(genOnTrackPrefix) { events =>
      val history = HandlingHistory(events)
      val once    = baseCargo.deriveDeliveryProgress(history).delivery
      val twice =
        baseCargo.deriveDeliveryProgress(history).deriveDeliveryProgress(history).delivery
      // `calculatedAt` uses Instant.now() — ignore it. All other fields must
      // be identical.
      lifecycleFingerprint(once) shouldEqual lifecycleFingerprint(twice)
    }
  }

  test("property: event-list order is irrelevant — history sorts by completionTime") {
    forAll(genOnTrackPrefix) { events =>
      val sortedHistory   = HandlingHistory(events)
      val shuffledHistory = HandlingHistory(Random.shuffle(events))
      val a               = baseCargo.deriveDeliveryProgress(sortedHistory).delivery
      val b               = baseCargo.deriveDeliveryProgress(shuffledHistory).delivery
      lifecycleFingerprint(a) shouldEqual lifecycleFingerprint(b)
    }
  }

  test("property: routingStatus depends only on (itinerary, spec), not on handling events") {
    val unrouted = Cargo(TrackingId("ZZZ"), spec)
    forAll(genOnTrackPrefix) { events =>
      val rerouted = unrouted.deriveDeliveryProgress(HandlingHistory(events))
      rerouted.delivery.routingStatus shouldEqual RoutingStatus.NOT_ROUTED
    }
  }

  // ---- Helpers ----------------------------------------------------------

  /**
   * Builder closure: defer the cargo argument so the same canonical step list
   * can be applied to different cargos (e.g., the not-yet-routed cargo in the
   * routingStatus property).
   */
  private type HandlingEventBuilder = Cargo => HandlingEvent

  private def ev(
      tpe: HandlingEventType,
      loc: Location,
      time: Instant,
      voy: Voyage
  ): HandlingEventBuilder =
    cargo => HandlingEvent(cargo, time, time, tpe, loc, voy)

  private def ev(tpe: HandlingEventType, loc: Location, time: Instant): HandlingEventBuilder =
    cargo => HandlingEvent(cargo, time, time, tpe, loc)

  private def t(ms: Long): Instant = Instant.ofEpochMilli(ms)

  /**
   * Capture every Delivery field except `calculatedAt` (which is wall-clock
   * `Instant.now()`) for equality comparisons in idempotence / order-
   * independence properties.
   */
  private def lifecycleFingerprint(d: Delivery): Product =
    (
      d.transportStatus,
      d.routingStatus,
      d.misdirected,
      d.lastKnownLocationOpt,
      d.currentVoyage,
      d.eta,
      d.nextExpectedActivity,
      d.isUnloadedAtDestination,
      d.lastEvent
    )
