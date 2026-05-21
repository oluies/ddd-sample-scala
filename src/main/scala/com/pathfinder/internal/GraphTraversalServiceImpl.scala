package com.pathfinder.internal

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Properties

import scala.collection.mutable
import scala.util.Random

import com.pathfinder.api.{GraphTraversalService, TransitEdge, TransitPath}

/**
 * In-process implementation of the routing graph traversal. Mirrors the
 * upstream Java reference: returns a small random set of plausible paths
 * by walking a shuffled subset of all known nodes between the origin and
 * destination. Each leg's date is offset from the previous one by ~1 day
 * with a ±500-minute jitter.
 *
 * This is *not* a real shortest-path algorithm — it's a fixture for the
 * teaching example.
 */
final class GraphTraversalServiceImpl(dao: GraphDAO) extends GraphTraversalService:

  private val random = new Random()

  override def findShortestPath(
      originNode: String,
      destinationNode: String,
      limitations: Properties
  ): List[TransitPath] =
    val candidateCount = 3 + random.nextInt(3)
    val candidates     = mutable.ListBuffer.empty[TransitPath]

    for _ <- 0 until candidateCount do
      val intermediates = randomChunk(
        dao.listAllNodes().filterNot(n => n == originNode || n == destinationNode)
      )
      val edges = mutable.ListBuffer.empty[TransitEdge]

      var fromNode = originNode
      var date     = Instant.now()
      val nodes    = intermediates :+ destinationNode
      for toNode <- nodes do
        val fromDate = nextDate(date)
        val toDate   = nextDate(fromDate)
        edges += TransitEdge(
          dao.getTransitEdge(fromNode, toNode),
          fromNode,
          toNode,
          fromDate,
          toDate
        )
        fromNode = toNode
        date = nextDate(toDate)

      candidates += TransitPath(edges.toList)

    candidates.toList

  private def nextDate(date: Instant): Instant =
    date.plus(1, ChronoUnit.DAYS).plus((random.nextInt(1000) - 500).toLong, ChronoUnit.MINUTES)

  private def randomChunk(nodes: List[String]): List[String] =
    val shuffled = random.shuffle(nodes)
    val size     = if shuffled.size > 4 then 1 + random.nextInt(5) else shuffled.size
    shuffled.take(size)
