package se.citerus.dddsample.interfaces.tracking.ws

/** DTO view of a handling event for the public tracking REST API. */
final case class HandlingEventDTO(
    location: String,
    time: String,
    `type`: String,
    voyageNumber: String,
    isExpected: Boolean,
    description: String
)
