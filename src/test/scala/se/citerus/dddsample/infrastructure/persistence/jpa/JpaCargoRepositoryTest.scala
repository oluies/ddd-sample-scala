package se.citerus.dddsample.infrastructure.persistence.jpa

import java.time.Instant

import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.{Bean, Import}
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.TestPropertySource

import se.citerus.dddsample.domain.model.cargo.*
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
 * Integration test for the JPA-based Cargo persistence layer.
 *
 * `@DataJpaTest` slices the Spring context to JPA + Spring Data only, and
 * brings up an embedded H2 in-memory database with Hibernate's
 * `create-drop` schema generation. Each test runs in its own transaction
 * which rolls back at teardown, isolating fixtures.
 *
 * `TestContextManager` bridges Spring's test-context machinery to
 * ScalaTest — `@Autowired` fields populate in `beforeAll`.
 *
 * The `spring.autoconfigure.exclude=` override clears the global exclude
 * list from `application.properties` (which disables JPA in production
 * until we wire it). Without this override Hibernate doesn't start.
 */
@DataJpaTest
@TestPropertySource(properties = Array("spring.autoconfigure.exclude="))
@Import(Array(classOf[JpaCargoRepositoryTest.TestConfig]))
class JpaCargoRepositoryTest extends AnyFunSuite with Matchers with BeforeAndAfterAll:

  // @Autowired field populated by TestContextManager in beforeAll.
  // Typed as the domain interface, not the concrete `final class
  // JpaCargoRepository`, because `spring.aop.proxy-target-class=false`
  // makes Spring wrap the @Transactional adapter in a JDK interface-based
  // proxy. The proxy implements `CargoRepository` but isn't a
  // `JpaCargoRepository`.
  @Autowired private var repository: CargoRepository = scala.compiletime.uninitialized

  override def beforeAll(): Unit =
    new TestContextManager(getClass).prepareTestInstance(this)

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

  test("find returns None for unknown tracking id") {
    repository.find(TrackingId("ABSENT")) shouldBe None
  }

  test("store + find round-trips an un-routed cargo") {
    val cargo = Cargo(TrackingId("ABC"), RouteSpecification(SHA, GOT, deadline))
    repository.store(cargo)

    val loaded = repository.find(TrackingId("ABC")).get
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
    repository.store(cargo)

    val loaded = repository.find(TrackingId("XYZ")).get
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
    repository.store(initial)
    repository.find(TrackingId("MUT")).get.itineraryOpt.get.legs should have size 1

    val rerouted = repository
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
    repository.store(rerouted)

    repository.find(TrackingId("MUT")).get.itineraryOpt.get.legs should have size 2
  }

  test("getAll returns every persisted cargo") {
    val c1 = Cargo(TrackingId("ONE"), RouteSpecification(SHA, GOT, deadline))
    val c2 = Cargo(TrackingId("TWO"), RouteSpecification(SHA, RTM, deadline))
    repository.store(c1)
    repository.store(c2)

    val ids = repository.getAll.map(_.trackingId.idString).toSet
    ids should contain allOf ("ONE", "TWO")
  }

object JpaCargoRepositoryTest:

  /**
   * Test-only Spring config: provides the JpaCargoRepository under test
   * plus mocked collaborator repositories. `@DataJpaTest` doesn't
   * auto-scan `@Repository`-annotated classes outside the JPA slice, so
   * we wire them by hand here.
   */
  @TestConfiguration
  class TestConfig:

    private val SHA = Location(UnLocode("CNSHA"), "Shanghai")
    private val RTM = Location(UnLocode("NLRTM"), "Rotterdam")
    private val GOT = Location(UnLocode("SEGOT"), "Gothenburg")

    private val voyage = new Voyage(
      VoyageNumber("V1"),
      Schedule(
        List(
          CarrierMovement(SHA, RTM, Instant.ofEpochMilli(1), Instant.ofEpochMilli(2)),
          CarrierMovement(RTM, GOT, Instant.ofEpochMilli(3), Instant.ofEpochMilli(4))
        )
      )
    )

    @Bean def locationRepository: LocationRepository =
      val m = mock(classOf[LocationRepository])
      when(m.find(UnLocode("CNSHA"))).thenReturn(Some(SHA))
      when(m.find(UnLocode("NLRTM"))).thenReturn(Some(RTM))
      when(m.find(UnLocode("SEGOT"))).thenReturn(Some(GOT))
      m

    @Bean def voyageRepository: VoyageRepository =
      val m = mock(classOf[VoyageRepository])
      when(m.find(VoyageNumber("V1"))).thenReturn(Some(voyage))
      m

    @Bean def handlingEventRepository: HandlingEventRepository =
      val m = mock(classOf[HandlingEventRepository])
      Seq("ABC", "XYZ", "MUT", "ONE", "TWO").foreach { id =>
        when(m.lookupHandlingHistoryOfCargo(TrackingId(id))).thenReturn(HandlingHistory.EMPTY)
      }
      m

    @Bean def cargoMapper(
        locationRepository: LocationRepository,
        voyageRepository: VoyageRepository,
        handlingEventRepository: HandlingEventRepository
    ): CargoMapper = new CargoMapper(locationRepository, voyageRepository, handlingEventRepository)

    @Bean def jpaCargoRepository(
        entityRepository: CargoEntityRepository,
        mapper: CargoMapper
    ): JpaCargoRepository = new JpaCargoRepository(entityRepository, mapper)
