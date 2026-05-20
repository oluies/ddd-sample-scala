package se.citerus.dddsample.interfaces.handling.ws

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}

import se.citerus.dddsample.application.ApplicationEvents
import se.citerus.dddsample.interfaces.handling.HandlingReportParser

/** Validates and parses incoming handling reports, then forwards each
  * resulting attempt to [[ApplicationEvents]].
  */
@RestController
final class HandlingReportServiceImpl(applicationEvents: ApplicationEvents) extends HandlingReportService:

  private val logger = LoggerFactory.getLogger(getClass)

  @PostMapping(
    path     = Array("/handlingReport"),
    produces = Array(MediaType.APPLICATION_JSON_VALUE),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  override def submitReport(@Valid @RequestBody handlingReport: HandlingReport): ResponseEntity[?] =
    try
      val attempts = HandlingReportParser.parse(handlingReport)
      attempts.foreach(applicationEvents.receivedHandlingEventRegistrationAttempt)
      ResponseEntity.status(HttpStatus.CREATED).build()
    catch
      case e: Exception =>
        logger.error("Unexpected error in submitReport", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(s"Internal server error: ${e.getMessage}")
