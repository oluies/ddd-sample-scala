package se.citerus.dddsample.domain.model.location

import se.citerus.dddsample.domain.shared.ValueObject
import java.util.regex.Pattern

/**
 * United nations location code.
 * <p/>
 * http://www.unece.org/cefact/locode/
 * http://www.unece.org/cefact/locode/DocColumnDescription.htm#LOCODE
 */
class UnLocode(val countryAndLocation:String) extends ValueObject[UnLocode] {
   // Location code is usually three letters, but may contain the numbers 2-9 as well
  private val VALID_PATTERN = Pattern.compile("[a-zA-Z]{2}[a-zA-Z2-9]{3}")
  
  require(countryAndLocation != null, "Country and location may not be null")
  require(VALID_PATTERN.matcher(countryAndLocation).matches(),
      countryAndLocation + " is not a valid UN/LOCODE (does not match pattern)");
      
  val idString = countryAndLocation.toUpperCase()
  
  override def equals(other:Any) : Boolean = other match {
    case other: UnLocode => other.getClass == getClass && sameValueAs(other)
    case _ => false 
  }
	
  override def hashCode = idString.hashCode
	  
	def sameValueAs(other:UnLocode) : Boolean = {
    return other != null && this.idString.equals(other.idString)
	}
  
  override def toString() = idString
}