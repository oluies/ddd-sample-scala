package se.citerus.dddsample.interfaces.tracking.ws

/** DTO view of a cargo for the public tracking REST API. */
final case class CargoTrackingDTO(
    trackingId: String,
    statusText: String,
    destination: String,
    eta: String,
    nextExpectedActivity: String,
    isMisdirected: Boolean,
    handlingEvents: List[HandlingEventDTO]
)
