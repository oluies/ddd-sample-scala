package se.citerus.dddsample.domain.model.voyage

trait VoyageRepository {
  /**
   * Finds a voyage using voyage number.
   *
   * @param voyageNumber voyage number
   * @return The voyage, or null if not found.
   */
  def find(voyageNumber:VoyageNumber) : Option[Voyage];

}