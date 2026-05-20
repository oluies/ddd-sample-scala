package se.citerus.dddsample.interfaces.booking.facade.dto

import java.io.Serializable

/**
 * A candidate route — the legs of one itinerary, suitable for presentation
 * and (later) selection by the booking clerk.
 */
final case class RouteCandidateDTO(legs: List[LegDTO]) extends Serializable
