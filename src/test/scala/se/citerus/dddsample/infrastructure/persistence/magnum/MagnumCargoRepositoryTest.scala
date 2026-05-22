package se.citerus.dddsample.infrastructure.persistence.magnum

import java.time.Instant
import javax.sql.DataSource

import scala.io.Source
import scala.util.Using

import org.h2.jdbcx.JdbcDataSource
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.cargo.{
  Cargo,
  Itinerary,
  Leg,
  RouteSpecification,
  RoutingStatus,
  TrackingId,
  TransportStatus
}
import se.citerus.dddsample.domain.model.handling.{HandlingEventRepository, HandlingHistory}
import se.citerus.dddsample.domain.model.location.{Location, LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{
  CarrierMovement,
  Schedule,
  Voyage,
  VoyageNumber,
  VoyageRepository
}

/**
 * Integration test for the Magnum-based Cargo persistence layer.
 *
 * Uses a real H2 in-memory database — *not* the in-memory map. Each test
 * gets a fresh schema; the location / voyage / handling-event collaborators
 * are mocked so the test stays focused on the persistence round-trip.
 *
 * The point: exercise the actual SQL Magnum generates + the Row ↔ Aggregate
 * mapper, end-to-end, against a real JDBC driver.
 */
class MagnumCargoRepositoryTest extends AnyFunSuite with Matchers with BeforeAndAfterEach:

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

  private var ds: DataSource                                   = scala.compiletime.uninitialized
  private var locationRepository: LocationRepository           = scala.compiletime.uninitialized
  private var voyageRepository: VoyageRepository               = scala.compiletime.uninitialized
  private var handlingEventRepository: HandlingEventRepository = scala.compiletime.uninitialized
  private var repo: MagnumCargoRepository                      = scala.compiletime.uninitialized

  override def beforeEach(): Unit =
    // Fresh in-memory H2 per test — DB_CLOSE_DELAY=-1 keeps the DB alive
    // for the duration of the JVM, plus a unique URL prevents cross-test
    // bleed if tests run in parallel.
    val dbName = s"mem:magnum-poc-${System.nanoTime}"
    val h2     = new JdbcDataSource
    // Plain H2 — no MODE=PostgreSQL because Magnum's H2DbType already
    // generates H2-native SQL.
    h2.setURL(s"jdbc:h2:$dbName;DB_CLOSE_DELAY=-1")
    h2.setUser("sa")
    ds = h2

    // Load schema. The file lives under src/test/resources/ which Mill
    // puts on the test classpath. H2's `Statement.execute` only runs one
    // statement at a time, so split on `;`.
    val schema = Using.resource(getClass.getResourceAsStream("/magnum-cargo-schema.sql")) { in =>
      Source.fromInputStream(in).mkString
    }
    Using.resource(ds.getConnection) { conn =>
      Using.resource(conn.createStatement) { st =>
        schema
          .split(";")
          .map(_.trim)
          .filter(_.nonEmpty)
          .foreach(st.execute)
      }
    }

    locationRepository = mock(classOf[LocationRepository])
    voyageRepository = mock(classOf[VoyageRepository])
    handlingEventRepository = mock(classOf[HandlingEventRepository])

    when(locationRepository.find(UnLocode("CNSHA"))).thenReturn(Some(SHA))
    when(locationRepository.find(UnLocode("NLRTM"))).thenReturn(Some(RTM))
    when(locationRepository.find(UnLocode("SEGOT"))).thenReturn(Some(GOT))
    when(voyageRepository.find(VoyageNumber("V1"))).thenReturn(Some(voyage))
    // Opaque-typed `TrackingId` can't be passed to Mockito's `any[T]`
    // (`classOf[opaque type]` isn't a class type). Stub each value the
    // tests exercise explicitly. Anything else returns null on the mock,
    // which surfaces as a clear NPE if we accidentally read an unstubbed
    // tracking id.
    Seq("ABC", "XYZ", "MUT", "ONE", "TWO").foreach { id =>
      when(handlingEventRepository.lookupHandlingHistoryOfCargo(TrackingId(id)))
        .thenReturn(HandlingHistory.EMPTY)
    }

    repo = new MagnumCargoRepository(
      ds,
      locationRepository,
      voyageRepository,
      handlingEventRepository
    )

  test("nextTrackingId mints distinct ids") {
    val a = repo.nextTrackingId()
    val b = repo.nextTrackingId()
    a should not equal b
  }

  test("find returns None for unknown tracking id") {
    repo.find(TrackingId("ABSENT")) shouldBe None
  }

  test("store + find round-trips an un-routed cargo") {
    val cargo = Cargo(TrackingId("ABC"), RouteSpecification(SHA, GOT, deadline))
    repo.store(cargo)

    val loaded = repo.find(TrackingId("ABC")).get
    loaded.trackingId.idString shouldEqual "ABC"
    loaded.origin shouldEqual SHA
    loaded.routeSpecification.destination shouldEqual GOT
    loaded.routeSpecification.arrivalDeadline shouldEqual deadline
    loaded.itineraryOpt shouldBe None
    loaded.delivery.routingStatus shouldEqual RoutingStatus.NOT_ROUTED
    loaded.delivery.transportStatus shouldEqual TransportStatus.NOT_RECEIVED
  }

  test("store + find round-trips a routed cargo with two legs") {
    val itinerary = Itinerary(
      List(
        Leg(voyage, SHA, RTM, Instant.ofEpochMilli(10), Instant.ofEpochMilli(20)),
        Leg(voyage, RTM, GOT, Instant.ofEpochMilli(30), Instant.ofEpochMilli(40))
      )
    )
    val cargo = Cargo(TrackingId("XYZ"), RouteSpecification(SHA, GOT, deadline))
      .assignToRoute(itinerary)

    repo.store(cargo)

    val loaded = repo.find(TrackingId("XYZ")).get
    loaded.itineraryOpt should not be empty
    loaded.itineraryOpt.get.legs should have size 2
    loaded.itineraryOpt.get.legs.head.loadLocation shouldEqual SHA
    loaded.itineraryOpt.get.legs.head.unloadLocation shouldEqual RTM
    loaded.itineraryOpt.get.legs(1).loadLocation shouldEqual RTM
    loaded.itineraryOpt.get.legs(1).unloadLocation shouldEqual GOT
    loaded.delivery.routingStatus shouldEqual RoutingStatus.ROUTED
  }

  test("store replaces an existing cargo's itinerary atomically") {
    val initial = Cargo(TrackingId("MUT"), RouteSpecification(SHA, GOT, deadline))
      .assignToRoute(
        Itinerary(List(Leg(voyage, SHA, RTM, Instant.ofEpochMilli(10), Instant.ofEpochMilli(20))))
      )
    repo.store(initial)
    repo.find(TrackingId("MUT")).get.itineraryOpt.get.legs should have size 1

    // Re-route with two legs; the existing leg row should be replaced.
    val rerouted = repo
      .find(TrackingId("MUT"))
      .get
      .assignToRoute(
        Itinerary(
          List(
            Leg(voyage, SHA, RTM, Instant.ofEpochMilli(10), Instant.ofEpochMilli(20)),
            Leg(voyage, RTM, GOT, Instant.ofEpochMilli(30), Instant.ofEpochMilli(40))
          )
        )
      )
    repo.store(rerouted)

    val loaded = repo.find(TrackingId("MUT")).get
    loaded.itineraryOpt.get.legs should have size 2
  }

  test("getAll returns every persisted cargo") {
    val c1 = Cargo(TrackingId("ONE"), RouteSpecification(SHA, GOT, deadline))
    val c2 = Cargo(TrackingId("TWO"), RouteSpecification(SHA, RTM, deadline))
    repo.store(c1)
    repo.store(c2)

    val all = repo.getAll
    all.map(_.trackingId.idString) should contain theSameElementsAs Seq("ONE", "TWO")
  }
