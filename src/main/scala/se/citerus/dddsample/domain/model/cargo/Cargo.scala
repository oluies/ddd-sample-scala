package se.citerus.dddsample.domain.model.cargo

;

import org.apache.commons.lang.Validate;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingHistory;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.Location;

//import se.citerus.dddsample.domain.shared.DomainObjectUtils;
import se.citerus.dddsample.domain.shared.Entity;

/**
 * A Cargo. This is the central class in the domain model,
 * and it is the root of the Cargo-Itinerary-Leg-Delivery-RouteSpecification aggregate.
 *
 * A cargo is identified by a unique tracking id, and it always has an origin
 * and a route specification. The life cycle of a cargo begins with the booking procedure,
 * when the tracking id is assigned. During a (short) period of time, between booking
 * and initial routing, the cargo has no itinerary.
 *
 * The booking clerk requests a list of possible routes, matching the route specification,
 * and assigns the cargo to one route. The route to which a cargo is assigned is described
 * by an itinerary.
 *
 * A cargo can be re-routed during transport, on demand of the customer, in which case
 * a new route is specified for the cargo and a new route is requested. The old itinerary,
 * being a value object, is discarded and a new one is attached.
 *
 * It may also happen that a cargo is accidentally misrouted, which should notify the proper
 * personnel and also trigger a re-routing procedure.
 *
 * When a cargo is handled, the status of the delivery changes. Everything about the delivery
 * of the cargo is contained in the Delivery value object, which is replaced whenever a cargo
 * is handled by an asynchronous event triggered by the registration of the handling event.
 *
 * The delivery can also be affected by routing changes, i.e. when a the route specification
 * changes, or the cargo is assigned to a new route. In that case, the delivery update is performed
 * synchronously within the cargo aggregate.
 *
 * The life cycle of a cargo ends when the cargo is claimed by the customer.
 *
 * The cargo aggregate, and the entire domain model, is built to solve the problem
 * of booking and tracking cargo. All important business rules for determining whether
 * or not a cargo is misdirected, what the current status of the cargo is (on board carrier,
 * in port etc), are captured in this aggregate.
 *
 */
class Cargo(val trackingId: TrackingId, private var mutableRouteSpecification: RouteSpecification) extends Entity[Cargo] {
  Validate.notNull(trackingId, "Tracking ID is required");
  Validate.notNull(mutableRouteSpecification, "Route specification is required");

  // Cargo origin never changes, even if the route specification changes.
  // However, at creation, cargo origin can be derived from the initial route specification.
  val origin = mutableRouteSpecification.origin

  private var mutableItinerary: Itinerary = Itinerary.EMPTY_ITINERARY
    
  def routeSpecification = { mutableRouteSpecification }
  def delivery = { mutableDelivery }
  def itinerary() = { mutableItinerary }

  private var mutableDelivery: Delivery = {
    Delivery.derivedFrom(mutableRouteSpecification, mutableItinerary, HandlingHistory.EMPTY);
  }
  
  /**
   * Attach a new itinerary to this cargo.
   *
   * @param itinerary an itinerary. May not be null.
   */
  def assignToRoute(newItinerary: Itinerary) : Unit = {
    Validate.notNull(newItinerary, "Itinerary is required for assignment");

    mutableItinerary = newItinerary;
    // Handling consistency within the Cargo aggregate synchronously
    mutableDelivery = mutableDelivery.updateOnRouting(mutableRouteSpecification, mutableItinerary);
  }
  
  /**
   * Updates all aspects of the cargo aggregate status
   * based on the current route specification, itinerary and handling of the cargo.
   * <p/>
   * When either of those three changes, i.e. when a new route is specified for the cargo,
   * the cargo is assigned to a route or when the cargo is handled, the status must be
   * re-calculated.
   * <p/>
   * {@link RouteSpecification} and {@link Itinerary} are both inside the Cargo
   * aggregate, so changes to them cause the status to be updated <b>synchronously</b>,
   * but changes to the delivery history (when a cargo is handled) cause the status update
   * to happen <b>asynchronously</b> since {@link HandlingEvent} is in a different aggregate.
   *
   * @param handlingHistory handling history
   */
  def deriveDeliveryProgress(handlingHistory:HandlingHistory) : Unit = {
    mutableDelivery = Delivery.derivedFrom(routeSpecification, mutableItinerary, handlingHistory)
  }
  
    /**
   * Specifies a new route for this cargo.
   *
   * @param routeSpecification route specification.
   */
  def specifyNewRoute(routeSpecification:RouteSpecification) : Unit = {
    Validate.notNull(routeSpecification, "Route specification is required");

    mutableRouteSpecification = routeSpecification;
    // Handling consistency within the Cargo aggregate synchronously
    mutableDelivery = mutableDelivery.updateOnRouting(mutableRouteSpecification, mutableItinerary);
  }
  
  def sameIdentityAs(other: Cargo): Boolean = {
    other != null && trackingId.sameValueAs(other.trackingId)
  }
  
  override def hashCode : Int = {
    trackingId.hashCode
  }
  
  override def equals(other:Any) : Boolean = other match {
    case other: Cargo => other.getClass == getClass && sameIdentityAs(other)
    case _ => false
  }

  override def toString : String = {
    trackingId.toString
  }
  
}