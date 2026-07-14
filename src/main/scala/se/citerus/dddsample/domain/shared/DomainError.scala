package se.citerus.dddsample.domain.shared

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber

/**
 * Sum type for *expected* domain-layer failures — the ones we want callers to
 * handle on the unhappy path, not catch from a thrown exception.
 *
 * Application services and the domain factories return
 * `Either[DomainError, A]`; controllers map each case to an HTTP status at the
 * boundary (see `interfaces/.../DomainErrorResponses`).
 *
 * `IllegalStateException` etc. (from `Objects.requireNonNull` on opaque types,
 * or from a corrupt persistence row) intentionally still throw — those signal
 * bugs / data corruption, not normal control flow, and shouldn't be in the
 * return type.
 */
enum DomainError:

  /** No cargo with this tracking id exists. */
  case UnknownCargo(trackingId: TrackingId)

  /** No location with this UN/Locode exists. */
  case UnknownLocation(unLocode: UnLocode)

  /** No voyage with this number exists. */
  case UnknownVoyage(voyageNumber: VoyageNumber)

  /**
   * A domain invariant rejected the request — e.g. origin equals destination,
   * or a handling-event type / voyage combination is forbidden. `cause` is
   * the underlying `IllegalArgumentException` from a `require` or smart
   * constructor; we keep it for logging but the controller-side mapping
   * shows only `message`.
   */
  case InvariantViolation(violationMessage: String, cause: Option[Throwable])

  /** Human-friendly description, safe to surface to API clients. */
  def message: String = this match
    case UnknownCargo(id) =>
      s"No cargo with tracking id ${id.idString} exists in the system"
    case UnknownLocation(loc) =>
      s"No location with UN locode ${loc.idString} exists in the system"
    case UnknownVoyage(v) =>
      s"No voyage with number ${v.idString} exists in the system"
    case InvariantViolation(msg, _) => msg

object DomainError:
  def invariantViolation(message: String): DomainError =
    InvariantViolation(message, None)

  def fromThrowable(t: Throwable): DomainError =
    InvariantViolation(Option(t.getMessage).getOrElse(t.getClass.getSimpleName), Some(t))
