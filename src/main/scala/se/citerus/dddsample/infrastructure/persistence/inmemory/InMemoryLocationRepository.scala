package se.citerus.dddsample.infrastructure.persistence.inmemory

import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters.*

import org.springframework.stereotype.Repository

import se.citerus.dddsample.domain.model.location.{Location, LocationRepository, UnLocode}

/** In-memory [[LocationRepository]] keyed by UN/LOCODE. */
@Repository
final class InMemoryLocationRepository extends LocationRepository:

  private val locations = new ConcurrentHashMap[String, Location]()

  override def find(unLocode: UnLocode): Option[Location] =
    Option(locations.get(unLocode.idString))

  override def getAll(): List[Location] = locations.values.asScala.toList

  override def store(location: Location): Location =
    locations.put(location.unLocode.idString, location)
    location
