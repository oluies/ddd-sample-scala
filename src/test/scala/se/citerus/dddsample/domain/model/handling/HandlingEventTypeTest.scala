package se.citerus.dddsample.domain.model.handling

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class HandlingEventTypeTest extends AnyFunSuite with Matchers:

  test("LOAD and UNLOAD require a voyage") {
    HandlingEventType.LOAD.requiresVoyage   shouldBe true
    HandlingEventType.UNLOAD.requiresVoyage shouldBe true
  }

  test("RECEIVE, CLAIM, CUSTOMS prohibit a voyage") {
    HandlingEventType.RECEIVE.prohibitsVoyage shouldBe true
    HandlingEventType.CLAIM.prohibitsVoyage   shouldBe true
    HandlingEventType.CUSTOMS.prohibitsVoyage shouldBe true
  }

  test("requiresVoyage and prohibitsVoyage are exhaustive complements") {
    HandlingEventType.values.foreach { t =>
      (t.requiresVoyage ^ t.prohibitsVoyage) shouldBe true
    }
  }

  test("sameValueAs reflects identity equality") {
    HandlingEventType.LOAD.sameValueAs(HandlingEventType.LOAD)   shouldBe true
    HandlingEventType.LOAD.sameValueAs(HandlingEventType.UNLOAD) shouldBe false
  }
