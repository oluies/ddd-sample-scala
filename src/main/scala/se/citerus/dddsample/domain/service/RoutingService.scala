package se.citerus.dddsample.domain.service

import se.citerus.dddsample.domain.model.cargo.Itinerary;
import se.citerus.dddsample.domain.model.cargo.RouteSpecification;

trait RoutingService {

  /**
   * @param routeSpecification route specification
   * @return A list of itineraries that satisfy the specification. May be an empty list if no route is found.
   */
  def fetchRoutesForSpecification(routeSpecification : RouteSpecification) : List[Itinerary];

}