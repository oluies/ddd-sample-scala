package com.pathfinder.internal

/** Routing-team's data-access port. */
trait GraphDAO:
  def listAllNodes(): List[String]
  def getTransitEdge(from: String, to: String): String
