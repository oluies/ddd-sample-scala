package se.citerus.dddsample.interfaces.booking.facade.internal.assembler

import se.citerus.dddsample.domain.model.location.Location
import se.citerus.dddsample.interfaces.booking.facade.dto.LocationDTO

final class LocationDTOAssembler:

  def toDTO(location: Location): LocationDTO =
    LocationDTO(location.unLocode.idString, location.name)

  def toDTOList(locations: List[Location]): List[LocationDTO] =
    locations.map(toDTO)
