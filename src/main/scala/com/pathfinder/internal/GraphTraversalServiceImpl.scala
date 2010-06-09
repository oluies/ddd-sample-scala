package com.pathfinder.internal

import com.pathfinder.api.GraphTraversalService
import util.Random;
import com.pathfinder.api.TransitEdge;
import com.pathfinder.api.TransitPath;

import java.util.Properties;
import java.util.Date

object GraphTraversalServiceImpl {
  val ONE_MIN_MS = 1000 * 60;
  val ONE_DAY_MS = ONE_MIN_MS * 60 * 24;
  
}

class GraphTraversalServiceImpl(dao: GraphDAO) extends GraphTraversalService {
 
  
  def findShortestPath(originUnLocode: String,
                       destinationUnLocode: String,
                       limitations: Properties): List[TransitPath] = {
    val trimmedVertices = dao.listLocations.remove((n) => (n == originUnLocode) || (n == destinationUnLocode))
    val candidateCount = getRandomNumberOfCandidates
    
    var candidates: List[TransitPath] = List()    
    var date = nextDate(new Date());
    for (i <- 0 to candidateCount) {
      val randomLocations = getRandomChunkOfLocations(trimmedVertices)
      val firstLegTo = trimmedVertices(0)

      var fromDate:Date = nextDate(date)
      var toDate = nextDate(fromDate)
      date = nextDate(toDate)

      val firstVoyageNumber = dao.getVoyageNumber(originUnLocode, firstLegTo)
      var transitEdges = List(new TransitEdge(firstVoyageNumber, originUnLocode, firstLegTo, fromDate, toDate))

      val randomSizeCount = (randomLocations.size - 1)
      for (j <- 0 to randomSizeCount) {
        val curr = randomLocations(j)
        val next = randomLocations(j + 1)
        fromDate = nextDate(date)
        toDate = nextDate(fromDate)
        date = nextDate(toDate)
        val voyageNumber = dao.getVoyageNumber(curr, next)
        val transitEdge = new TransitEdge(voyageNumber, curr, next, fromDate, toDate)
        transitEdges = transitEdges ::: List(transitEdge)
      }

      val lastLegFrom = randomLocations.last
      fromDate = nextDate(date)
      toDate = nextDate(fromDate)
      val voyageNumber = dao.getVoyageNumber(lastLegFrom, destinationUnLocode)
      val transitEdge = new TransitEdge(voyageNumber, lastLegFrom, destinationUnLocode, fromDate, toDate)
      transitEdges = transitEdges ::: List(transitEdge)

      candidates = candidates ::: List(new TransitPath(transitEdges))
    }

    candidates
  }
  
  private def nextDate(date: Date): Date = {
    val random = new Random  
    new Date(date.getTime() + GraphTraversalServiceImpl.ONE_DAY_MS + (random.nextInt(1000) - 500) * GraphTraversalServiceImpl.ONE_MIN_MS)
  }

  private def getRandomNumberOfCandidates = {
    val random = new Random
    3 + random.nextInt(3)
  }

  private def getRandomChunkOfLocations(allLocations: List[String]): List[String] = {
    val locations = scala.util.Random.shuffle(allLocations)
    val total = locations.size
    val random = new Random
    val chunk = if (total > 4) 1 + random.nextInt(5) else total
    locations.slice(0, chunk)
  }
}