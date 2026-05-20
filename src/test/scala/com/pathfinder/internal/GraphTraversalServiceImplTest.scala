package com.pathfinder.internal

import java.util.Properties

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class GraphTraversalServiceImplTest extends AnyFunSuite with Matchers:

  test("findShortestPath returns at least 3 candidate paths starting at origin and ending at destination") {
    val svc   = new GraphTraversalServiceImpl(new GraphDAOStub)
    val paths = svc.findShortestPath("CNHKG", "USNYC", new Properties())

    paths.size should be >= 3
    paths.foreach { p =>
      p.transitEdges.head.fromNode shouldEqual "CNHKG"
      p.transitEdges.last.toNode   shouldEqual "USNYC"
      p.transitEdges.size          should be >= 1
    }
  }
