package se.citerus.dddsample.interfaces.booking.facade.internal.assembler

import se.citerus.dddsample.interfaces.booking.facade.dto._

import se.citerus.dddsample.domain.model.cargo._
import se.citerus.dddsample.domain.model.voyage._
import se.citerus.dddsample.domain.model.location._

object ItineraryCandidateDTOAssembler {

  /**
   * @param itinerary itinerary
   * @return A route candidate DTO
   */
   def toDTO(itinerary:Itinerary) : RouteCandidateDTO = {
    var legDTOs = List[LegDTO]()
    for (val leg <- itinerary.legs) {
      legDTOs = legDTOs ::: List(toLegDTO(leg));
    }
    return new RouteCandidateDTO(legDTOs);
  }

  /**
   * @param leg leg
   * @return A leg DTO
   */
  def toLegDTO(leg:Leg) :  LegDTO = {
    val voyageNumber = leg.voyage.voyageNumber
    val from = leg.loadLocation.unlocode
    val to = leg.unloadLocation.unlocode
    return new LegDTO(voyageNumber.idString, from.idString, to.idString, leg.loadTime, leg.unloadTime);
  }

  /**
   * @param routeCandidateDTO route candidate DTO
   * @param voyageRepository voyage repository
   * @param locationRepository location repository
   * @return An itinerary
   */
  def fromDTO(routeCandidateDTO:RouteCandidateDTO,
              voyageRepository:VoyageRepository,
              locationRepository:LocationRepository) : Itinerary = {
    var legs:List[Leg] = List[Leg]();
    for (val legDTO <- routeCandidateDTO.legs) {
      val voyageNumber = new VoyageNumber(legDTO.voyageNumber);
      val voyage = voyageRepository.find(voyageNumber).get;
      val from = locationRepository.find(new UnLocode(legDTO.from)).get;
      val to = locationRepository.find(new UnLocode(legDTO.to)).get;
      legs = legs ::: List(new Leg(voyage, from, to, legDTO.loadTime, legDTO.unloadTime));
    }
    return Itinerary(legs);
  }
}