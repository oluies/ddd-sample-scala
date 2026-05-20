package com.pathfinder.api

import java.io.Serializable
import java.time.Instant

/** An edge in a path through the routing graph — one leg of a transit path.
  *
  * Belongs to the *routing team's* bounded context. UN/LOCODEs and voyage
  * numbers are plain `String`s here, deliberately so: the routing team
  * doesn't know about our [[se.citerus.dddsample.domain.model.location.UnLocode]]
  * value object. The translation happens in
  * [[se.citerus.dddsample.infrastructure.routing.ExternalRoutingService]].
  */
final case class TransitEdge(
    edge: String,      // voyage number
    fromNode: String,  // UN/LOCODE of origin
    toNode: String,    // UN/LOCODE of destination
    fromDate: Instant,
    toDate: Instant
) extends Serializable
