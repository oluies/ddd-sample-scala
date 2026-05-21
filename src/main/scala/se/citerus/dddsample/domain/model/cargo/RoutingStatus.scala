package se.citerus.dddsample.domain.model.cargo

import se.citerus.dddsample.domain.shared.ValueObject

/** Routing status. */
enum RoutingStatus extends ValueObject[RoutingStatus]:
  case NOT_ROUTED, ROUTED, MISROUTED

  override def sameValueAs(other: RoutingStatus): Boolean = this == other
