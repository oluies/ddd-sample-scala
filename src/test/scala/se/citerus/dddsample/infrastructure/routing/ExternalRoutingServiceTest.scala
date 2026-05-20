package se.citerus.dddsample.infrastructure.routing

import java.time.Instant
import java.util.Properties

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import com.pathfinder.api.{GraphTraversalService, TransitEdge, TransitPath}

import se.citerus.dddsample.domain.model.cargo.RouteSpecification
import se.citerus.dddsample.infrastructure.persistence.inmemory.{
  InMemoryLocationRepository,
  InMemoryVoyageRepository
}
import se.citerus.dddsample.infrastructure.sampledata.{SampleLocations, SampleVoyages}

class ExternalRoutingServiceTest extends AnyFunSuite with Matchers:

  test("translates pathfinder transit paths into domain itineraries") {
    val locationRepo = new InMemoryLocationRepository
    SampleLocations.all.foreach(locationRepo.store)

    val voyageRepo = new InMemoryVoyageRepository
    SampleVoyages.all.foreach(voyageRepo.store)

    val edge = TransitEdge(
      edge = SampleVoyages.HONGKONG_TO_NEW_YORK.voyageNumber.idString,
      fromNode = SampleLocations.HONGKONG.unLocode.idString,
      toNode = SampleLocations.NEWYORK.unLocode.idString,
      fromDate = Instant.parse("2008-10-01T12:00:00Z"),
      toDate = Instant.parse("2008-10-23T23:10:00Z")
    )
    val stubGraph: GraphTraversalService =
      (_: String, _: String, _: Properties) => List(TransitPath(List(edge)))

    val routing = new ExternalRoutingService(stubGraph, locationRepo, voyageRepo)
    val spec    = RouteSpecification(SampleLocations.HONGKONG, SampleLocations.NEWYORK, Instant.parse("2008-11-01T00:00:00Z"))

    val itineraries = routing.fetchRoutesForSpecification(spec)
    itineraries should have size 1
    itineraries.head.legs should have size 1
    itineraries.head.legs.head.loadLocation   shouldEqual SampleLocations.HONGKONG
    itineraries.head.legs.head.unloadLocation shouldEqual SampleLocations.NEWYORK
  }
