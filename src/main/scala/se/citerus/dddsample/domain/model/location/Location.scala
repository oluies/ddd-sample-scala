package se.citerus.dddsample.domain.model.location

import java.util.Objects

import se.citerus.dddsample.domain.shared.DomainEntity

/**
 * A location in our model is a stop on a journey, such as a cargo origin or
 * destination, or carrier-movement endpoints. Uniquely identified by a UN
 * Locode.
 *
 * Pure domain class: no JPA / Hibernate annotations. The JPA persistence
 * model lives in `infrastructure.persistence.jpa` (phase 9) and converts to
 * / from this type via a mapper.
 */
final class Location private (val unLocode: UnLocode, val name: String)
    extends DomainEntity[Location]:

  override def sameIdentityAs(other: Location): Boolean =
    other != null && this.unLocode == other.unLocode

  override def equals(o: Any): Boolean = o match
    case that: Location => sameIdentityAs(that)
    case _              => false

  override def hashCode: Int = unLocode.hashCode

  override def toString: String = s"$name [${unLocode.idString}]"

object Location:

  /** Special Location object that marks an unknown location. */
  val UNKNOWN: Location = new Location(UnLocode("XXXXX"), "Unknown location")

  /** Construct a Location. */
  def apply(unLocode: UnLocode, name: String): Location =
    Objects.requireNonNull(unLocode, "unLocode must not be null")
    Objects.requireNonNull(name, "name must not be null")
    new Location(unLocode, name)
