package com.pathfinder.internal

import scala.util.Random

/** Hard-coded stub graph data. Matches the upstream Java fixture. */
final class GraphDAOStub extends GraphDAO:

  private val random = new Random()

  override def listAllNodes(): List[String] =
    List(
      "CNHKG",
      "AUMEL",
      "SESTO",
      "FIHEL",
      "USCHI",
      "JNTKO",
      "DEHAM",
      "CNSHA",
      "NLRTM",
      "SEGOT",
      "CNHGH",
      "USNYC",
      "USDAL"
    )

  override def getTransitEdge(from: String, to: String): String =
    random.nextInt(5) match
      case 0 => "0100S"
      case 1 => "0200T"
      case 2 => "0300A"
      case 3 => "0301S"
      case _ => "0400S"
