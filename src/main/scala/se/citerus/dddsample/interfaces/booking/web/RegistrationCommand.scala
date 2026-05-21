package se.citerus.dddsample.interfaces.booking.web

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

/** Request body for `POST /admin/cargos` — origin, destination, deadline. */
final case class RegistrationCommand @JsonCreator() (
    @JsonProperty("originUnlocode") originUnlocode: String,
    @JsonProperty("destinationUnlocode") destinationUnlocode: String,
    @JsonProperty("arrivalDeadline") arrivalDeadline: String
)
