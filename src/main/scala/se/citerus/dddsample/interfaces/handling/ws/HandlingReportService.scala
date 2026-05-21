package se.citerus.dddsample.interfaces.handling.ws

import org.springframework.http.ResponseEntity

trait HandlingReportService:
  def submitReport(handlingReport: HandlingReport): ResponseEntity[?]
