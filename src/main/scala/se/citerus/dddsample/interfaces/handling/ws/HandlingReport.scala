package se.citerus.dddsample.interfaces.handling.ws

import java.time.LocalDateTime
import java.util.List as JList

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

/**
 * Request body for `POST /handlingReport` — an attempt to register one or
 * more handling events for a list of cargo tracking ids.
 *
 * Immutable Scala case class. Jackson constructs it via the `@JsonCreator`
 * annotated constructor — `spring-boot-starter-web` ships the Jackson
 * `parameter-names` module out of the box, so JSON keys bind to the
 * constructor params by name.
 *
 * The `trackingIds` field stays as the raw Java `List` because the JMS
 * payload serialization (in [[se.citerus.dddsample.interfaces.handling.HandlingReportParser]])
 * uses the same DTO shape and we don't want Scala-collection classes on
 * the wire.
 */
final case class HandlingReport @JsonCreator() (
    @JsonProperty(value = "completionTime", required = true) completionTime: LocalDateTime,
    @JsonProperty(value = "trackingIds", required = true) trackingIds: JList[String],
    @JsonProperty(value = "type", required = true) `type`: String,
    @JsonProperty(value = "unLocode", required = true) unLocode: String,
    @JsonProperty("voyageNumber") voyageNumber: String
)
