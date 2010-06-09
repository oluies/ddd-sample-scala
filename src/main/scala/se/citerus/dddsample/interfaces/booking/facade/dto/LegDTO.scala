package se.citerus.dddsample.interfaces.booking.facade.dto

import java.util.Date

case class LegDTO(val voyageNumber:String, val from:String, val to:String, val loadTime:Date, val unloadTime:Date) {
}