package se.citerus.dddsample.interfaces.booking.facade.internal.assembler

import se.citerus.dddsample.domain.model.cargo.{Itinerary, Leg}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}
import se.citerus.dddsample.interfaces.booking.facade.dto.{LegDTO, RouteCandidateDTO}

final class ItineraryCandidateDTOAssembler:

  def toDTO(itinerary: Itinerary): RouteCandidateDTO =
    RouteCandidateDTO(itinerary.legs.map(toLegDTO))

  private def toLegDTO(leg: Leg): LegDTO =
    LegDTO(
      leg.voyage.voyageNumber.idString,
      leg.loadLocation.unLocode.idString,
      leg.unloadLocation.unLocode.idString,
      leg.loadTime,
      leg.unloadTime
    )

  def fromDTO(
      dto: RouteCandidateDTO,
      voyageRepository: VoyageRepository,
      locationRepository: LocationRepository
  ): Itinerary =
    val legs = dto.legs.map { legDTO =>
      val voyage = voyageRepository.find(VoyageNumber(legDTO.voyageNumber)).getOrElse(
        throw new NoSuchElementException(s"Unknown voyage ${legDTO.voyageNumber}")
      )
      val from = locationRepository.find(UnLocode(legDTO.from)).getOrElse(
        throw new NoSuchElementException(s"Unknown origin ${legDTO.from}")
      )
      val to = locationRepository.find(UnLocode(legDTO.to)).getOrElse(
        throw new NoSuchElementException(s"Unknown destination ${legDTO.to}")
      )
      Leg(voyage, from, to, legDTO.loadTime, legDTO.unloadTime)
    }
    Itinerary(legs)
