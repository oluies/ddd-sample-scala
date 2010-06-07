package se.citerus.dddsample.domain.model.location

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.shared.Entity;

class Location(val unlocode: UnLocode, val name: String) extends Entity[Location] {
  Validate.notNull(unlocode);
  Validate.notNull(name);

  override def sameIdentityAs(other: Location): Boolean = {
    unlocode.sameValueAs(other.unlocode);
  }

  override def equals(other: Any): Boolean = other match {
    case other: Location => other.getClass == getClass && sameIdentityAs(other)
    case _ => false
  }

  /**
   * @return Hash code of UN locode.
   */
  override def hashCode(): Int = {unlocode.hashCode()}

  override def toString(): String = {
    return name + " [" + unlocode + "]"
  }

}

object Location {
  /**
   * Special Location object that marks an unknown location.
   */
  val UNKNOWN: Location = new Location(new UnLocode("XXXXX"), "Unknown location");
}