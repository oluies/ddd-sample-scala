package se.citerus.dddsample.interfaces.booking.facade.internal.assembler

import se.citerus.dddsample.interfaces.booking.facade.dto._

import se.citerus.dddsample.domain.model.cargo._


object CargoRoutingDTOAssembler {
  
  /**
   *
   * @param cargo cargo
   * @return A cargo routing DTO
   */
  def toDTO(cargo:Cargo) : CargoRoutingDTO = {
    val dto = new CargoRoutingDTO(
      cargo.trackingId.idString,
      cargo.origin.unlocode.idString,
      cargo.routeSpecification.destination.unlocode.idString,
      cargo.routeSpecification.arrivalDeadline,
      cargo.delivery.routingStatus.sameValueAs(MISROUTED));
    for (leg <- cargo.itinerary.legs) {
      dto.addLeg(
        leg.voyage.voyageNumber.idString,
        leg.loadLocation.unlocode.idString,
        leg.unloadLocation.unlocode.idString,
        leg.loadTime(),
        leg.unloadTime());
    }
    return dto;
  }
  
}