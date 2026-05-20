package se.citerus.dddsample.interfaces.tracking.ws

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

import org.springframework.context.MessageSource

import se.citerus.dddsample.domain.model.cargo.Cargo
import se.citerus.dddsample.domain.model.handling.{HandlingEvent, HandlingEventType}

/** Converts a domain [[Cargo]] and its handling events into the public
  * tracking [[CargoTrackingDTO]]. Locale-sensitive description text is
  * resolved via Spring's [[MessageSource]] (keys live in
  * `messages*.properties`).
  */
object CargoTrackingDTOConverter:

  private val formatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MMM d, uuuu, h:mm:ss a", Locale.ENGLISH)
    .withZone(ZoneOffset.UTC)

  def convert(
      cargo: Cargo,
      handlingEvents: List[HandlingEvent],
      messageSource: MessageSource,
      locale: Locale
  ): CargoTrackingDTO =
    val handlingEventDTOs = handlingEvents.map { he =>
      HandlingEventDTO(
        location     = he.location.name,
        time         = he.completionTime.toString,
        `type`       = he.eventType.toString,
        voyageNumber = he.voyage.voyageNumber.idString,
        isExpected   = cargo.itinerary.isExpected(he),
        description  = describe(he, messageSource, locale)
      )
    }

    CargoTrackingDTO(
      trackingId           = cargo.trackingId.idString,
      statusText           = statusText(cargo, messageSource, locale),
      destination          = cargo.routeSpecification.destination.name,
      eta                  = cargo.delivery.eta.map(_.toString).getOrElse("Unknown"),
      nextExpectedActivity = nextExpectedActivity(cargo),
      isMisdirected        = cargo.delivery.isMisdirected,
      handlingEvents       = handlingEventDTOs
    )

  private def describe(he: HandlingEvent, ms: MessageSource, locale: Locale): String =
    val args: Array[AnyRef] = he.eventType match
      case HandlingEventType.LOAD | HandlingEventType.UNLOAD =>
        Array(he.voyage.voyageNumber.idString, he.location.name, formatter.format(he.completionTime))
      case HandlingEventType.RECEIVE | HandlingEventType.CUSTOMS | HandlingEventType.CLAIM =>
        Array(he.location.name, formatter.format(he.completionTime))
    ms.getMessage(s"deliveryHistory.eventDescription.${he.eventType.toString}", args, locale)

  private def statusText(cargo: Cargo, ms: MessageSource, locale: Locale): String =
    val delivery = cargo.delivery
    val code     = s"cargo.status.${delivery.transportStatus.toString}"
    val args: Array[AnyRef] = delivery.transportStatus.toString match
      case "IN_PORT"          => Array(delivery.lastKnownLocation.name)
      case "ONBOARD_CARRIER"  => Array(delivery.currentVoyage.voyageNumber.idString)
      case _                  => null
    ms.getMessage(code, args, "[Unknown status]", locale)

  private def nextExpectedActivity(cargo: Cargo): String =
    cargo.delivery.nextExpectedActivity match
      case None       => ""
      case Some(act)  =>
        val prefix = "Next expected activity is to "
        val tname  = act.eventType.toString.toLowerCase
        act.eventType match
          case HandlingEventType.LOAD =>
            s"${prefix}$tname cargo onto voyage ${act.voyage.map(_.voyageNumber.idString).getOrElse("")} in ${act.location.name}"
          case HandlingEventType.UNLOAD =>
            s"${prefix}$tname cargo off of ${act.voyage.map(_.voyageNumber.idString).getOrElse("")} in ${act.location.name}"
          case _ =>
            s"${prefix}$tname cargo in ${act.location.name}"
