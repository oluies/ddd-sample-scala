package se.citerus.dddsample.application.domain.model.cargo

import se.citerus.dddsample.domain.shared.ValueObject

final class TrackingId(val id:String) extends ValueObject[TrackingId] {
	
  /**
   * @return String representation of this tracking id.
   */
  def idString() : String = id

  override def equals(o : Any) : Boolean = {
		//    if (this == o) return true;
		//    if (o == null || getClass() != o.getClass()) return false;
		//
		//    val other = (TrackingId) o;
		//
		//    return sameValueAs(other);
	  false
  }
  
  override def sameValueAs(other:TrackingId) : Boolean = {
	//  other != null && this.id.equals(other.id)
	  false
  }

  override def hashCode() : Int = id.hashCode()

  override def toString() = id

}