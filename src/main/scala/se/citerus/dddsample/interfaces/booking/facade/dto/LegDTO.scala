package se.citerus.dddsample.interfaces.booking.facade.dto

import java.io.Serializable
import java.time.Instant

/** DTO for one leg of an itinerary. */
final case class LegDTO(
    voyageNumber: String,
    from: String,
    to: String,
    loadTime: Instant,
    unloadTime: Instant
) extends Serializable
