package se.citerus.dddsample.infrastructure.routing

import org.apache.commons.logging.Log
import se.citerus.dddsample.domain.model.cargo._;
import org.apache.commons.logging.LogFactory;
import com.pathfinder.api.GraphTraversalService
import java.util.Properties
import java.rmi.RemoteException;
import com.pathfinder.api.TransitEdge;
import com.pathfinder.api.TransitPath;
import se.citerus.dddsample.domain.model.cargo.Itinerary;
import se.citerus.dddsample.domain.model.cargo.Leg;
import se.citerus.dddsample.domain.model.cargo.RouteSpecification;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;
import se.citerus.dddsample.domain.service.RoutingService;

class ExternalRoutingService extends RoutingService {
  
  private val logger = LogFactory.getLog(getClass());
  
  var graphTraversalService: GraphTraversalService = _
  var locationRepository: LocationRepository = _
  var voyageRepository: VoyageRepository = _

  def fetchRoutesForSpecification(routeSpecification:RouteSpecification) : List[Itinerary] = {
    /*
      The RouteSpecification is picked apart and adapted to the external API.
     */
    val origin = routeSpecification.origin;
    val destination = routeSpecification.destination

    val limitations = new Properties();
    limitations.setProperty("DEADLINE", routeSpecification.arrivalDeadline.toString());
   
    try {
       var transitPaths = graphTraversalService.findShortestPath(origin.unlocode.idString, destination.unlocode.idString, limitations)
        
      /*
       The returned result is then translated back into our domain model.
      */
      var itineraries:List[Itinerary] = List()
      for (transitPath <- transitPaths) {
        val itinerary = toItinerary(transitPath);
        // Use the specification to safe-guard against invalid itineraries
        if (routeSpecification.isSatisfiedBy(itinerary)) {
          itineraries = itineraries ::: List(itinerary);
        } else {
          logger.warn("Received itinerary that did not satisfy the route specification");
        }
      }
      return itineraries;
    } catch {
      case e:RemoteException => {
        logger.error(e, e);
        return List();
      }
      case e:Exception => { throw e }
    }
  }

  private def toItinerary(transitPath:TransitPath) : Itinerary = {
    var legs:List[Leg] = List()
    for (edge:TransitEdge <- transitPath.transitEdges) {
      legs = legs ::: List(toLeg(edge));
    }
    return Itinerary(legs);
  }

  private def toLeg(edge:TransitEdge) : Leg = {
    val voyageNumber = new VoyageNumber(edge.voyageNumber)
    val voyage = voyageRepository.find(voyageNumber).get
    val fromLocation = locationRepository.find(new UnLocode(edge.fromUnLocode)).get
    val toLocation = locationRepository.find(new UnLocode(edge.toUnLocode)).get
    return new Leg(voyage, fromLocation, toLocation, edge.fromDate, edge.toDate);
  }

}