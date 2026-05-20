package se.citerus.dddsample.scenario

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.application.ApplicationEvents
import se.citerus.dddsample.application.impl.{
  BookingServiceImpl,
  CargoInspectionServiceImpl,
  HandlingEventServiceImpl
}
import se.citerus.dddsample.application.util.DateUtils.toDate
import se.citerus.dddsample.domain.model.cargo.{
  Cargo,
  Itinerary,
  Leg,
  RoutingStatus,
  TransportStatus
}
import se.citerus.dddsample.domain.model.handling.{
  HandlingEvent,
  HandlingEventFactory,
  HandlingEventType
}
import se.citerus.dddsample.domain.service.RoutingService
import se.citerus.dddsample.infrastructure.persistence.inmemory.{
  InMemoryCargoRepository,
  InMemoryHandlingEventRepository,
  InMemoryLocationRepository,
  InMemoryVoyageRepository
}
import se.citerus.dddsample.infrastructure.sampledata.{SampleLocations, SampleVoyages}
import se.citerus.dddsample.interfaces.handling.HandlingEventRegistrationAttempt

/** End-to-end scenario: a cargo is booked, routed, handled, inspected, and
  * misdirection / arrival events fire as expected.
  *
  * Wires the *real* application services to the *in-memory* infrastructure
  * implementations from phase 9a — no Spring context, no JMS broker, no
  * database. The synchronous `ApplicationEvents` stub records callbacks
  * so the test can assert they fire at the right times.
  */
class CargoLifecycleScenarioTest extends AnyFunSuite with Matchers:

  // --- Infrastructure ----------------------------------------------------
  private val cargoRepository         = new InMemoryCargoRepository
  private val locationRepository      = new InMemoryLocationRepository
  private val voyageRepository        = new InMemoryVoyageRepository
  private val handlingEventRepository = new InMemoryHandlingEventRepository

  SampleLocations.all.foreach(locationRepository.store)
  SampleVoyages.all.foreach(voyageRepository.store)

  // --- Application events stub (synchronous) -----------------------------
  private val handledCount     = new AtomicInteger(0)
  private val misdirectedCount = new AtomicInteger(0)
  private val arrivedCount     = new AtomicInteger(0)

  private val applicationEvents = new ApplicationEvents:
    override def cargoWasHandled(event: HandlingEvent): Unit                     = { handledCount.incrementAndGet(); () }
    override def cargoWasMisdirected(cargo: Cargo): Unit                         = { misdirectedCount.incrementAndGet(); () }
    override def cargoHasArrived(cargo: Cargo): Unit                             = { arrivedCount.incrementAndGet(); () }
    override def receivedHandlingEventRegistrationAttempt(attempt: HandlingEventRegistrationAttempt): Unit = ()

  // --- Domain factory + application services -----------------------------
  private val handlingEventFactory =
    new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository)

  // Trivial routing service: returns one itinerary derived from the named
  // sample voyages. A real routing service would call the pathfinder.
  private val routingService = new RoutingService:
    override def fetchRoutesForSpecification(spec: se.citerus.dddsample.domain.model.cargo.RouteSpecification): List[Itinerary] =
      List(itineraryFor(spec))

  private val cargoFactory = new se.citerus.dddsample.domain.model.cargo.CargoFactory(locationRepository, cargoRepository)
  private val bookingService = new BookingServiceImpl(cargoRepository, locationRepository, routingService, cargoFactory)
  private val handlingEventService =
    new HandlingEventServiceImpl(handlingEventRepository, applicationEvents, handlingEventFactory)
  private val cargoInspectionService =
    new CargoInspectionServiceImpl(applicationEvents, cargoRepository, handlingEventRepository)

  private def itineraryFor(spec: se.citerus.dddsample.domain.model.cargo.RouteSpecification): Itinerary =
    import SampleLocations.*
    import SampleVoyages.*
    Itinerary(List(
      Leg(HONGKONG_TO_NEW_YORK, HONGKONG, NEWYORK, toDate("2009-03-02"), toDate("2009-03-05")),
      Leg(NEW_YORK_TO_DALLAS,   NEWYORK,  DALLAS,  toDate("2009-03-06"), toDate("2009-03-08")),
      Leg(DALLAS_TO_HELSINKI,   DALLAS,   STOCKHOLM, toDate("2009-03-09"), toDate("2009-03-12"))
    ))

  test("cargo lifecycle: book, route, handle, inspect → arrives at destination") {
    import SampleLocations.*
    import SampleVoyages.*

    // 1. Booking
    val trackingId = bookingService.bookNewCargo(
      HONGKONG.unLocode, STOCKHOLM.unLocode, toDate("2009-03-18")
    )
    cargoRepository.find(trackingId) shouldBe defined

    // 2. Routing — pick the only itinerary and assign it.
    val candidates = bookingService.requestPossibleRoutesForCargo(trackingId)
    candidates                                            should not be empty
    val itinerary = candidates.head
    bookingService.assignCargoToRoute(itinerary, trackingId)

    val cargo = cargoRepository.find(trackingId).get
    cargo.delivery.routingStatus shouldEqual RoutingStatus.ROUTED

    // 3. Receive at Hong Kong.
    handlingEventService.registerHandlingEvent(
      toDate("2009-03-01"), trackingId, None, HONGKONG.unLocode, HandlingEventType.RECEIVE
    )
    cargoInspectionService.inspectCargo(trackingId)
    val cargoAfterReceive = cargoRepository.find(trackingId).get
    cargoAfterReceive.delivery.transportStatus shouldEqual TransportStatus.IN_PORT
    cargoAfterReceive.delivery.lastKnownLocation shouldEqual HONGKONG

    // 4. Load onto Hong Kong → New York.
    handlingEventService.registerHandlingEvent(
      toDate("2009-03-02"), trackingId, Some(HONGKONG_TO_NEW_YORK.voyageNumber),
      HONGKONG.unLocode, HandlingEventType.LOAD
    )
    cargoInspectionService.inspectCargo(trackingId)
    cargoRepository.find(trackingId).get.delivery.transportStatus shouldEqual TransportStatus.ONBOARD_CARRIER

    // 5. Unload at New York.
    handlingEventService.registerHandlingEvent(
      toDate("2009-03-05"), trackingId, Some(HONGKONG_TO_NEW_YORK.voyageNumber),
      NEWYORK.unLocode, HandlingEventType.UNLOAD
    )
    cargoInspectionService.inspectCargo(trackingId)
    cargoRepository.find(trackingId).get.delivery.transportStatus shouldEqual TransportStatus.IN_PORT

    // We've recorded three handling events; each fired cargoWasHandled once.
    handledCount.get shouldEqual 3
    misdirectedCount.get shouldEqual 0
    arrivedCount.get     shouldEqual 0

    // 6. Misdirection: unload at the wrong port.
    handlingEventService.registerHandlingEvent(
      toDate("2009-03-06"), trackingId, Some(NEW_YORK_TO_DALLAS.voyageNumber),
      HELSINKI.unLocode, HandlingEventType.UNLOAD
    )
    cargoInspectionService.inspectCargo(trackingId)
    cargoRepository.find(trackingId).get.delivery.isMisdirected shouldBe true
    misdirectedCount.get shouldEqual 1
  }
