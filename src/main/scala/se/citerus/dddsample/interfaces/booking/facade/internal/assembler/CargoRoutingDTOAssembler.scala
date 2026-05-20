package se.citerus.dddsample.interfaces.booking.facade.internal.assembler

import se.citerus.dddsample.domain.model.cargo.{Cargo, RoutingStatus}
import se.citerus.dddsample.interfaces.booking.facade.dto.{CargoRoutingDTO, LegDTO}

final class CargoRoutingDTOAssembler:

  def toDTO(cargo: Cargo): CargoRoutingDTO =
    val legs = cargo.itinerary.legs.map { leg =>
      LegDTO(
        leg.voyage.voyageNumber.idString,
        leg.loadLocation.unLocode.idString,
        leg.unloadLocation.unLocode.idString,
        leg.loadTime,
        leg.unloadTime
      )
    }
    CargoRoutingDTO(
      cargo.trackingId.idString,
      cargo.origin.unLocode.idString,
      cargo.routeSpecification.destination.unLocode.idString,
      cargo.routeSpecification.arrivalDeadline,
      cargo.delivery.routingStatus.sameValueAs(RoutingStatus.MISROUTED),
      legs
    )
