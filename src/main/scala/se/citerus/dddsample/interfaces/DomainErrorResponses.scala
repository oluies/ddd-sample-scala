package se.citerus.dddsample.interfaces

import org.springframework.http.{HttpStatus, ResponseEntity}

import se.citerus.dddsample.domain.shared.DomainError

/**
 * Edge-layer translation of [[DomainError]] → HTTP response. The application
 * services return `Either[DomainError, A]`; controllers `.fold` over the
 * result to either produce an OK / 204 response or one of these errors.
 *
 *   - `UnknownCargo` / `UnknownLocation` / `UnknownVoyage` → 404
 *   - `InvariantViolation` → 400
 *
 * Body shape is `{"error": "<message>"}` — the same shape the existing
 * `HandlingInterfacesExceptionHandler` uses for legacy
 * `IllegalArgumentException`-driven 400s.
 */
object DomainErrorResponses:

  /** Map a DomainError to a JSON error ResponseEntity at the right HTTP status. */
  def toResponseEntity(err: DomainError): ResponseEntity[Map[String, String]] =
    ResponseEntity.status(statusFor(err)).body(Map("error" -> err.message))

  /** Map a DomainError to a `ResponseEntity[?]` so it can flow into a wildcard return type. */
  def toAnyResponse(err: DomainError): ResponseEntity[?] = toResponseEntity(err)

  private def statusFor(err: DomainError): HttpStatus = err match
    case DomainError.UnknownCargo(_)          => HttpStatus.NOT_FOUND
    case DomainError.UnknownLocation(_)       => HttpStatus.NOT_FOUND
    case DomainError.UnknownVoyage(_)         => HttpStatus.NOT_FOUND
    case DomainError.InvariantViolation(_, _) => HttpStatus.BAD_REQUEST
