package se.citerus.dddsample.domain.model.cargo

trait CargoRepository {

  /**
   * Finds a cargo using given id.
   *
   * @param trackingId Id
   * @return Cargo if found, else   { @code null }
   */
  def find(trackingId: TrackingId): Option[Cargo];

  /**
   * Finds all cargo.
   *
   * @return All cargo.
   */
  def findAll(): List[Cargo];

  /**
   * Saves given cargo.
   *
   * @param cargo cargo to save
   */
  def store(cargo: Cargo): Unit;

  /**
   * @return A unique, generated tracking Id.
   */
  def nextTrackingId(): TrackingId;
}