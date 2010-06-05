package se.citerus.dddsample.domain.model.voyage

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.shared.ValueObject;

class VoyageNumber(val number:String) extends ValueObject[VoyageNumber] {

  override def sameValueAs(other:VoyageNumber) : Boolean = {
    return other != null && number.equals(other.number);
  }

}