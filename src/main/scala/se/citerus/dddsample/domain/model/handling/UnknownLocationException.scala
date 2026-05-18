package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.location.UnLocode

/** Thrown when registering an event refers to an unknown UN/Locode. */
final class UnknownLocationException(val unlocode: UnLocode) extends CannotCreateHandlingEventException:
  override def getMessage: String =
    s"No location with UN locode ${unlocode.idString} exists in the system"
