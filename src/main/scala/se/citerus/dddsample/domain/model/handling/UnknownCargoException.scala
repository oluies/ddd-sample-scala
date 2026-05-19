package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.cargo.TrackingId

/** Thrown when registering an event refers to an unknown tracking id. */
final class UnknownCargoException(val trackingId: TrackingId) extends CannotCreateHandlingEventException:
  override def getMessage: String =
    s"No cargo with tracking id ${trackingId.idString} exists in the system"
