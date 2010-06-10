package se.citerus.dddsample.interfaces.booking.facade.internal.assembler

import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.interfaces.booking.facade.dto._

object LocationDTOAssembler {
 
  def toDTO(location:Location) : LocationDTO = {
    new LocationDTO(location.unlocode.idString, location.name);
  }
  
  def toDTOList(allLocations:List[Location]) : List[LocationDTO] = {
    var dtoList = List[LocationDTO]()
    for (val location <- allLocations) {
      dtoList = dtoList ::: List(toDTO(location));
    }
    return dtoList;
  }
}