package se.citerus.dddsample.domain.service

import se.citerus.dddsample.domain.model.cargo.{Itinerary, RouteSpecification}

/**
 * Routing service — a domain-layer abstraction over the pathfinder. Returns
 * the itineraries that satisfy a given [[RouteSpecification]]. Implementing
 * adapters live in `infrastructure.routing` (phase 10).
 */
trait RoutingService:

  /**
   * @return itineraries satisfying the specification, or an empty list if
   *         no route is found.
   */
  def fetchRoutesForSpecification(routeSpecification: RouteSpecification): List[Itinerary]
