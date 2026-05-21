package se.citerus.dddsample.infrastructure.messaging.jms

import java.time.Instant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber
import se.citerus.dddsample.interfaces.handling.HandlingEventRegistrationAttempt

/**
 * Locks the JMS payload format: the `HandlingEventRegistrationAttempt`
 * case class must round-trip through Jackson JSON cleanly with the
 * `JavaTimeModule` + `DefaultScalaModule` configuration the JMS bean uses.
 *
 * If this test breaks, the JMS consumer can no longer read the messages
 * the producer writes — silently dropping handling events in production.
 */
class HandlingEventRegistrationAttemptJsonTest extends AnyFunSuite with Matchers:

  private val mapper: ObjectMapper =
    val m = new ObjectMapper
    m.registerModule(new JavaTimeModule)
    m.registerModule(DefaultScalaModule)
    m

  private val withVoyage = HandlingEventRegistrationAttempt(
    registrationTime = Instant.parse("2026-05-21T10:00:00Z"),
    completionTime = Instant.parse("2026-05-21T08:00:00Z"),
    trackingId = TrackingId("ABC123"),
    voyageNumber = Some(VoyageNumber("V-100")),
    eventType = HandlingEventType.LOAD,
    unLocode = UnLocode("CNHKG")
  )

  private val withoutVoyage = HandlingEventRegistrationAttempt(
    registrationTime = Instant.parse("2026-05-21T10:00:00Z"),
    completionTime = Instant.parse("2026-05-21T08:00:00Z"),
    trackingId = TrackingId("XYZ999"),
    voyageNumber = None,
    eventType = HandlingEventType.RECEIVE,
    unLocode = UnLocode("USNYC")
  )

  test("LOAD event with Some(voyage) round-trips through JSON") {
    val json    = mapper.writeValueAsString(withVoyage)
    val decoded = mapper.readValue(json, classOf[HandlingEventRegistrationAttempt])
    decoded shouldEqual withVoyage
  }

  test("RECEIVE event with None voyage round-trips through JSON") {
    val json    = mapper.writeValueAsString(withoutVoyage)
    val decoded = mapper.readValue(json, classOf[HandlingEventRegistrationAttempt])
    decoded shouldEqual withoutVoyage
  }

  test("JSON contains no Java class hints — payload is plain data") {
    val json = mapper.writeValueAsString(withVoyage)
    json should not include "@class"
    json should not include "@type"
    json should not include "se.citerus"
    json should not include "scala."
  }
