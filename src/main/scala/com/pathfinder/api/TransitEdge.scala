package com.pathfinder.api

import java.util.Date

/**
 * Represents an edge in a path through a graph,
 * describing the route of a cargo.
 *
 */
case class TransitEdge(val voyageNumber: String,
                       val fromUnLocode: String,
                       val toUnLocode: String,
                       val fromDate: Date,
                       val toDate: Date) extends java.io.Serializable {
}