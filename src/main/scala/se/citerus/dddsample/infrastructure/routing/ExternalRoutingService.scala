package se.citerus.dddsample.infrastructure.routing

import java.util.Properties

import com.pathfinder.api.{GraphTraversalService, TransitEdge, TransitPath}
import org.slf4j.LoggerFactory

import se.citerus.dddsample.domain.model.cargo.{Itinerary, Leg, RouteSpecification}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}
import se.citerus.dddsample.domain.service.RoutingService

/**
 * Our end of the routing service. Translates between our domain model and
 * the routing-team API ([[GraphTraversalService]]).
 */
final class ExternalRoutingService(
    graphTraversalService: GraphTraversalService,
    locationRepository: LocationRepository,
    voyageRepository: VoyageRepository
) extends RoutingService:

  private val logger = LoggerFactory.getLogger(getClass)

  override def fetchRoutesForSpecification(spec: RouteSpecification): List[Itinerary] =
    val limitations = new Properties()
    limitations.setProperty("DEADLINE", spec.arrivalDeadline.toString)

    val transitPaths = graphTraversalService.findShortestPath(
      spec.origin.unLocode.idString,
      spec.destination.unLocode.idString,
      limitations
    )

    transitPaths
      .map(toItinerary)
      .filter { i =>
        if spec.isSatisfiedBy(i) then true
        else
          logger.warn("Received itinerary that did not satisfy the route specification")
          false
      }

  private def toItinerary(path: TransitPath): Itinerary =
    Itinerary(path.transitEdges.map(toLeg))

  private def toLeg(edge: TransitEdge): Leg =
    val voyage = voyageRepository
      .find(VoyageNumber(edge.edge))
      .getOrElse(
        throw new NoSuchElementException(s"Unknown voyage ${edge.edge}")
      )
    val from = locationRepository
      .find(UnLocode(edge.fromNode))
      .getOrElse(
        throw new NoSuchElementException(s"Unknown origin ${edge.fromNode}")
      )
    val to = locationRepository
      .find(UnLocode(edge.toNode))
      .getOrElse(
        throw new NoSuchElementException(s"Unknown destination ${edge.toNode}")
      )
    Leg(voyage, from, to, edge.fromDate, edge.toDate)
