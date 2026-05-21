package se.citerus.dddsample.domain.shared

/**
 * An entity, as explained in the DDD book.
 *
 * Entities compare by identity, not by attributes.
 */
trait DomainEntity[T]:
  /**
   * @param other The other entity.
   * @return true if the identities are the same, regardless of other attributes.
   */
  def sameIdentityAs(other: T): Boolean
