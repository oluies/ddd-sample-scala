package se.citerus.dddsample.interfaces.booking.facade.dto

import java.io.Serializable

/** Location DTO — UN/LOCODE and human-readable name. */
final case class LocationDTO(unLocode: String, name: String) extends Serializable
