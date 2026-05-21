package se.citerus.dddsample.interfaces.tracking.ws

import jakarta.servlet.http.HttpServletRequest

import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RestController}
import org.springframework.web.servlet.support.RequestContextUtils

import se.citerus.dddsample.domain.model.cargo.{CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository

@RestController
final class CargoTrackingRestService(
    cargoRepository: CargoRepository,
    handlingEventRepository: HandlingEventRepository,
    messageSource: MessageSource
):
  private val logger = LoggerFactory.getLogger(getClass)

  @GetMapping(
    path = Array("/api/track/{trackingId}"),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  def trackCargo(
      request: HttpServletRequest,
      @PathVariable("trackingId") trackingId: String
  ): ResponseEntity[CargoTrackingDTO] =
    try
      val locale = RequestContextUtils.getLocale(request)
      val trkId  = TrackingId(trackingId)
      cargoRepository.find(trkId) match
        case None => ResponseEntity.notFound().build()
        case Some(cargo) =>
          val events = handlingEventRepository
            .lookupHandlingHistoryOfCargo(trkId)
            .distinctEventsByCompletionTime
          ResponseEntity.ok(
            CargoTrackingDTOConverter.convert(cargo, events, messageSource, locale)
          )
    catch
      case e: Exception =>
        logger.error("Unexpected error in trackCargo endpoint", e)
        ResponseEntity.status(500).build()
