package com.aggregator

import javax.xml.datatype.XMLGregorianCalendar
import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.location.UnLocode

/**
 * Apparently some kind of XML parsing thingy.
 */
class HandlingReport(val completionTime:XMLGregorianCalendar,
                     val trackingIds:List[TrackingId],
                     val handlingReportType:String,
                     val unlocode:UnLocode,
                     val voyageNumber:String) {
  
}