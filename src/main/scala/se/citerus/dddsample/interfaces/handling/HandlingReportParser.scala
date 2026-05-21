package se.citerus.dddsample.interfaces.handling

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.List as JList

import scala.jdk.CollectionConverters.*

import se.citerus.dddsample.application.handling.HandlingEventRegistrationAttempt
import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber
import se.citerus.dddsample.interfaces.handling.ws.HandlingReport

/**
 * Utility for parsing incoming handling reports into
 * [[HandlingEventRegistrationAttempt]]s.
 */
object HandlingReportParser:

  def parseUnLocode(unlocode: String): UnLocode =
    try UnLocode(unlocode)
    catch
      case e @ (_: IllegalArgumentException | _: NullPointerException) =>
        throw new IllegalArgumentException(s"Failed to parse UNLO code: $unlocode", e)

  def parseTrackingId(trackingId: String): TrackingId =
    try TrackingId(trackingId)
    catch
      case e @ (_: IllegalArgumentException | _: NullPointerException) =>
        throw new IllegalArgumentException(s"Failed to parse trackingId: $trackingId", e)

  /**
   * Returns `None` for null or blank input — the upstream Java returns
   * nullable `VoyageNumber` to signal "no voyage". Scala uses `Option`.
   */
  def parseVoyageNumber(voyageNumber: String): Option[VoyageNumber] =
    if voyageNumber == null || voyageNumber.isBlank then None
    else
      try Some(VoyageNumber(voyageNumber))
      catch
        case e: IllegalArgumentException =>
          throw new IllegalArgumentException(s"Failed to parse voyage number: $voyageNumber", e)

  def parseEventType(eventType: String): HandlingEventType =
    try HandlingEventType.valueOf(eventType)
    catch
      case _: IllegalArgumentException =>
        throw new IllegalArgumentException(
          s"$eventType is not a valid handling event type. Valid types are: ${HandlingEventType.values.mkString("[", ", ", "]")}"
        )

  def parseCompletionTime(completionTime: LocalDateTime): Instant =
    if completionTime == null then throw new IllegalArgumentException("Completion time is required")
    Instant.ofEpochSecond(completionTime.toEpochSecond(ZoneOffset.UTC))

  def parseTrackingIds(trackingIdStrs: JList[String]): List[TrackingId] =
    Option(trackingIdStrs).map(_.asScala.toList).getOrElse(Nil).map(parseTrackingId)

  def parse(report: HandlingReport): List[HandlingEventRegistrationAttempt] =
    val completion  = parseCompletionTime(report.completionTime)
    val voyage      = parseVoyageNumber(report.voyageNumber)
    val eventType   = parseEventType(report.`type`)
    val unLocode    = parseUnLocode(report.unLocode)
    val trackingIds = parseTrackingIds(report.trackingIds)
    trackingIds.map { tid =>
      HandlingEventRegistrationAttempt(
        Instant.now(),
        completion,
        tid,
        voyage,
        eventType,
        unLocode
      )
    }
