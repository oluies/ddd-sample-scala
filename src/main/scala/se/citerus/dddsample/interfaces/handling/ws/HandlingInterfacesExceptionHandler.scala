package se.citerus.dddsample.interfaces.handling.ws

import org.slf4j.LoggerFactory
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{ExceptionHandler, RestControllerAdvice}

/**
 * Maps `IllegalArgumentException`s thrown by [[HandlingReportServiceImpl]] /
 * [[se.citerus.dddsample.interfaces.handling.HandlingReportParser]] to HTTP
 * 400 Bad Request.
 *
 * Without this, `HandlingReportParser`'s validation errors would surface
 * as the generic `catch (e: Exception)` in `submitReport` and become 500s.
 * The parser's messages echo only attacker-supplied input (e.g.
 * `"Failed to parse trackingId: XYZ"`) plus the closed `HandlingEventType`
 * enum — both already part of the public API surface — so it's safe to
 * include them in the 400 body.
 *
 * Scoped to the `interfaces.handling.ws` controller package so it doesn't
 * intercept exceptions from the booking or tracking controllers, which
 * have their own conventions.
 */
@RestControllerAdvice(basePackages = Array("se.citerus.dddsample.interfaces.handling.ws"))
final class HandlingInterfacesExceptionHandler:

  private val logger = LoggerFactory.getLogger(getClass)

  @ExceptionHandler(Array(classOf[IllegalArgumentException]))
  def handleIllegalArgument(e: IllegalArgumentException): ResponseEntity[Map[String, String]] =
    logger.info("Rejecting handling report: {}", e.getMessage)
    ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(Map("error" -> Option(e.getMessage).getOrElse("invalid request")))
