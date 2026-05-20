package se.citerus.dddsample.application.impl

import java.time.Instant

import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.application.ApplicationEvents
import se.citerus.dddsample.domain.model.cargo.{
  Cargo,
  CargoRepository,
  RouteSpecification,
  TrackingId
}
import se.citerus.dddsample.domain.model.handling.{
  HandlingEvent,
  HandlingEventFactory,
  HandlingEventRepository,
  HandlingEventType
}
import se.citerus.dddsample.domain.model.location.{Location, LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{
  CarrierMovement,
  Schedule,
  Voyage,
  VoyageNumber,
  VoyageRepository
}

class HandlingEventServiceImplTest extends AnyFunSuite with Matchers:

  private val HAMBURG   = Location(UnLocode("DEHAM"), "Hamburg")
  private val TOKYO     = Location(UnLocode("JPTYO"), "Tokyo")
  private val STOCKHOLM = Location(UnLocode("SESTO"), "Stockholm")

  private val voyage = new Voyage(
    VoyageNumber("CM001"),
    Schedule(
      List(CarrierMovement(STOCKHOLM, HAMBURG, Instant.ofEpochMilli(1), Instant.ofEpochMilli(2)))
    )
  )

  private val cargo = new Cargo(
    TrackingId("ABC"),
    RouteSpecification(HAMBURG, TOKYO, Instant.parse("2026-12-01T00:00:00Z"))
  )

  test("registerHandlingEvent stores the event and publishes cargoWasHandled") {
    val cargoRepository         = mock(classOf[CargoRepository])
    val voyageRepository        = mock(classOf[VoyageRepository])
    val handlingEventRepository = mock(classOf[HandlingEventRepository])
    val locationRepository      = mock(classOf[LocationRepository])
    val applicationEvents       = mock(classOf[ApplicationEvents])
    val factory = new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository)
    val service = new HandlingEventServiceImpl(handlingEventRepository, applicationEvents, factory)

    when(cargoRepository.find(cargo.trackingId)).thenReturn(Some(cargo))
    when(voyageRepository.find(voyage.voyageNumber)).thenReturn(Some(voyage))
    when(locationRepository.find(STOCKHOLM.unLocode)).thenReturn(Some(STOCKHOLM))

    service.registerHandlingEvent(
      Instant.now(),
      cargo.trackingId,
      Some(voyage.voyageNumber),
      STOCKHOLM.unLocode,
      HandlingEventType.LOAD
    )

    verify(handlingEventRepository, times(1)).store(isA(classOf[HandlingEvent]))
    verify(applicationEvents, times(1)).cargoWasHandled(isA(classOf[HandlingEvent]))
  }
