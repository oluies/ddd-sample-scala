package se.citerus.dddsample.infrastructure.persistence.inmemory

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.cargo.{Cargo, RouteSpecification, TrackingId}
import se.citerus.dddsample.domain.model.handling.{HandlingEvent, HandlingEventType}
import se.citerus.dddsample.domain.model.location.{Location, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{CarrierMovement, Schedule, Voyage, VoyageNumber}

class InMemoryRepositoriesTest extends AnyFunSuite with Matchers:

  private val SHA = Location(UnLocode("CNSHA"), "Shanghai")
  private val GOT = Location(UnLocode("SEGOT"), "Gothenburg")

  test("InMemoryLocationRepository round-trips by UN locode") {
    val repo = new InMemoryLocationRepository
    repo.find(UnLocode("CNSHA")) shouldBe None
    repo.store(SHA)
    repo.find(UnLocode("CNSHA")) shouldEqual Some(SHA)
    repo.getAll() should contain only SHA
  }

  test("InMemoryCargoRepository stores and mints fresh tracking ids") {
    val repo = new InMemoryCargoRepository
    val spec  = RouteSpecification(SHA, GOT, Instant.parse("2026-12-01T00:00:00Z"))
    val cargo = new Cargo(TrackingId("XYZ"), spec)
    repo.store(cargo)
    repo.find(TrackingId("XYZ")) shouldEqual Some(cargo)
    repo.find(TrackingId("UNKNOWN")) shouldBe None
    val id1 = repo.nextTrackingId()
    val id2 = repo.nextTrackingId()
    id1 should not equal id2
  }

  test("InMemoryVoyageRepository round-trips by voyage number") {
    val repo = new InMemoryVoyageRepository
    val voyage = new Voyage(
      VoyageNumber("V1"),
      Schedule(List(CarrierMovement(SHA, GOT, Instant.ofEpochMilli(1), Instant.ofEpochMilli(2))))
    )
    repo.store(voyage)
    repo.find(VoyageNumber("V1")) shouldEqual Some(voyage)
    repo.find(VoyageNumber("V999")) shouldBe None
  }

  test("InMemoryHandlingEventRepository scopes history to the requested cargo") {
    val repo  = new InMemoryHandlingEventRepository
    val spec  = RouteSpecification(SHA, GOT, Instant.parse("2026-12-01T00:00:00Z"))
    val c1    = new Cargo(TrackingId("C1"), spec)
    val c2    = new Cargo(TrackingId("C2"), spec)
    val now   = Instant.now()
    val e1    = HandlingEvent(c1, now, now, HandlingEventType.RECEIVE, SHA)
    val e2    = HandlingEvent(c2, now, now, HandlingEventType.RECEIVE, SHA)
    repo.store(e1)
    repo.store(e2)
    val history = repo.lookupHandlingHistoryOfCargo(TrackingId("C1"))
    history.distinctEventsByCompletionTime should contain only e1
  }
