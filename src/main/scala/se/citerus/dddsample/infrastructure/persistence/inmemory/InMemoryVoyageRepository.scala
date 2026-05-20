package se.citerus.dddsample.infrastructure.persistence.inmemory

import java.util.concurrent.ConcurrentHashMap

import org.springframework.stereotype.Repository

import se.citerus.dddsample.domain.model.voyage.{Voyage, VoyageNumber, VoyageRepository}

/** In-memory [[VoyageRepository]] keyed by voyage number. */
@Repository
final class InMemoryVoyageRepository extends VoyageRepository:

  private val voyages = new ConcurrentHashMap[String, Voyage]()

  override def find(voyageNumber: VoyageNumber): Option[Voyage] =
    Option(voyages.get(voyageNumber.idString))

  override def store(voyage: Voyage): Unit =
    voyages.put(voyage.voyageNumber.idString, voyage)
