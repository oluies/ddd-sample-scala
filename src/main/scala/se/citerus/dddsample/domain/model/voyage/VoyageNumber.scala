package se.citerus.dddsample.domain.model.voyage

/** Identifies a voyage.
  *
  * Opaque type over `String` (D2). The upstream Java reference requires only
  * non-null (no pattern check), so the smart constructor mirrors that.
  */
opaque type VoyageNumber = String

object VoyageNumber:
  def apply(number: String): VoyageNumber =
    require(number != null, "voyage number must not be null")
    number

  extension (v: VoyageNumber)
    def idString: String                       = v
    def sameValueAs(other: VoyageNumber): Boolean = v == other
