package se.citerus.dddsample.domain.model.location

import java.util.regex.Pattern

/**
 * United nations location code.
 *
 *   - http://www.unece.org/cefact/locode/
 *   - http://www.unece.org/cefact/locode/DocColumnDescription.htm#LOCODE
 *
 * Country code is exactly two letters; location code is usually three letters
 * but may contain the numbers 2-9 as well.
 *
 * Opaque type over `String` (D2): zero-allocation wrapper with smart
 * constructor enforcing the UN/LOCODE pattern. Equality and hashCode come
 * from the underlying `String`.
 */
opaque type UnLocode = String

object UnLocode:

  private val ValidPattern: Pattern =
    Pattern.compile("[a-zA-Z]{2}[a-zA-Z2-9]{3}")

  /**
   * Smart constructor. Throws [[IllegalArgumentException]] (which extends
   * [[RuntimeException]]) on null or invalid input; the upstream Java code
   * uses [[NullPointerException]] for null, but Scala's `require` makes
   * `IllegalArgumentException` more idiomatic.
   */
  def apply(countryAndLocation: String): UnLocode =
    require(countryAndLocation != null, "Country and location may not be null")
    require(
      ValidPattern.matcher(countryAndLocation).matches(),
      s"$countryAndLocation is not a valid UN/LOCODE (does not match pattern)"
    )
    countryAndLocation.toUpperCase

  extension (u: UnLocode)
    /** Country code and location code concatenated, always upper case. */
    def idString: String = u

    /** Value-equality semantics from the [[ValueObject]] contract. */
    def sameValueAs(other: UnLocode): Boolean = u == other
