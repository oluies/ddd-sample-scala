package se.citerus.dddsample.domain.model.cargo

/** Repository for the [[Cargo]] aggregate. */
trait CargoRepository:

  /** Finds a cargo by tracking id. `None` if not found (upstream Java returns
    * nullable `Cargo`).
    */
  def find(trackingId: TrackingId): Option[Cargo]

  /** Returns all cargo. */
  def getAll: List[Cargo]

  /** Persists the given cargo. */
  def store(cargo: Cargo): Unit

  /** Generates a fresh, unique tracking id. */
  def nextTrackingId(): TrackingId
