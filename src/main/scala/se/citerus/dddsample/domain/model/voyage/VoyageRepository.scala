package se.citerus.dddsample.domain.model.voyage

/** Repository for [[Voyage]] aggregates. */
trait VoyageRepository:

  /** Finds a voyage by number. Returns `None` if not found (the upstream Java
    * reference returns a nullable `Voyage`; Scala uses `Option`).
    */
  def find(voyageNumber: VoyageNumber): Option[Voyage]

  /** Persists a new or updated voyage. */
  def store(voyage: Voyage): Unit
