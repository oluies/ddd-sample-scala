package com.pathfinder.api

import java.util.Properties

/**
 * Part of the external graph-traversal API exposed by the routing team and
 * consumed by booking / tracking. Upstream Java extends `java.rmi.Remote` —
 * we drop that since the impl (phase 11) lives in-process and Java RMI is
 * effectively unused in modern Spring Boot deployments.
 */
trait GraphTraversalService:

  /**
   * @param origin       UN/LOCODE of origin
   * @param destination  UN/LOCODE of destination
   * @param limitations  free-form constraints (e.g. `DEADLINE`)
   * @return candidate transit paths (may be empty)
   */
  def findShortestPath(
      origin: String,
      destination: String,
      limitations: Properties
  ): List[TransitPath]
