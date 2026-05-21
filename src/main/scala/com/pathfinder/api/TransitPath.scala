package com.pathfinder.api

import java.io.Serializable

/** A path through the routing graph — an ordered list of [[TransitEdge]]s. */
final case class TransitPath(transitEdges: List[TransitEdge]) extends Serializable
