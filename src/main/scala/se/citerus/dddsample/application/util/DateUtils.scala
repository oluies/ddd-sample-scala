package se.citerus.dddsample.application.util

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Test-friendly helpers for parsing ISO-style date / date-time strings into
 * [[Instant]]s.
 */
object DateUtils:

  /** Parse `yyyy-MM-dd` as midnight UTC. */
  def toDate(date: String): Instant = toDate(date, "00:00")

  /** Parse `yyyy-MM-dd` + `HH:mm` as UTC. */
  def toDate(date: String, time: String): Instant =
    try Instant.parse(s"${date}T${time}:00Z")
    catch case e: DateTimeParseException => throw new RuntimeException(e)
