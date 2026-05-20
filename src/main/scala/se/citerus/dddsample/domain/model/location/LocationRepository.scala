package se.citerus.dddsample.domain.model.location

/** Repository for [[Location]] aggregates. */
trait LocationRepository:

  /**
   * Finds a location by UN Locode.
   *
   * Returns `None` if not found (the upstream Java reference returns the
   * raw `Location`, possibly `null`; Scala uses `Option`).
   */
  def find(unLocode: UnLocode): Option[Location]

  /** @return all known locations. */
  def getAll(): List[Location]

  /** Persists a new or updated location and returns it. */
  def store(location: Location): Location
