package se.citerus.dddsample.domain.model.handling

import se.citerus.dddsample.domain.model.voyage.VoyageNumber

/** Thrown when registering an event refers to an unknown voyage number. */
final class UnknownVoyageException(val voyageNumber: VoyageNumber)
    extends CannotCreateHandlingEventException:
  override def getMessage: String =
    s"No voyage with number ${voyageNumber.idString} exists in the system"
