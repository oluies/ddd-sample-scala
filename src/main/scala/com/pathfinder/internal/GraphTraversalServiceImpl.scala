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
    val date = nextDate(new Date());
    trimmedVertices = dao.listLocations.remove(originUnLocode).remove(destinationUnLocode)

    val candidateCount = getRandomNumberOfCandidates
    val candidates: List[TransitPath] = List()

    for (i <- 0 to candidateCount) {
      allVertices = getRandomChunkOfLocations(allVertices);
      val transitEdges: List[TransitEdge] = List()
      val firstLegTo = trimmedVertices(0)

      val fromDate = nextDate(date)
      val toDate = nextDate(fromDate)
      date = nextDate(toDate)

      transitEdges.add(new TransitEdge(
        dao.getVoyageNumber(originUnLocode, firstLegTo),
        originUnLocode, firstLegTo, fromDate, toDate));

      for (j <- 0..allVertices.size() - 1) {
        val curr = allVertices.get(j);
        fval next = allVertices.get(j + 1);
        fromDate = nextDate(date);
        toDate = nextDate(fromDate);
        date = nextDate(toDate);
        transitEdges.add(new TransitEdge(dao.getVoyageNumber(curr, next), curr, next, fromDate, toDate));
      }

      val lastLegFrom = allVertices.last;
      fromDate = nextDate(date);
      toDate = nextDate(fromDate);
      val transitEdge = new TransitEdge(dao.getVoyageNumber(lastLegFrom, destinationUnLocode),
        lastLegFrom, destinationUnLocode, fromDate, toDate)
      transitEdges.add(transitEdge);

      candidates.add(new TransitPath(transitEdges));
    }

    candidates
  }

  private def nextDate(date: Date): Date = {
    new Date(date.getTime() + ONE_DAY_MS + (random.nextInt(1000) - 500) * ONE_MIN_MS)
  }

  private def getRandomNumberOfCandidates = {
    return 3 + random.nextInt(3)
  }

  private def getRandomChunkOfLocations(allLocations: List[String]): List[String] = {
    locations = allLocations.shuffle;
    val chunk = total > 4 ? 1 + new Random().nextInt(5): locations.size;
    locations.subList(0, chunk);
  }
}