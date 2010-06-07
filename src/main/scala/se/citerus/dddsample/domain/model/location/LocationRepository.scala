package se.citerus.dddsample.domain.model.location

trait LocationRepository {

  /**
   * Finds a location using given unlocode.
   *
   * @param unLocode UNLocode.
   * @return Location.
   */
  def find(unLocode: UnLocode): Option[Location];

  /**
   * Finds all locations.
   *
   * @return All locations.
   */
  def findAll(): List[Location];

}