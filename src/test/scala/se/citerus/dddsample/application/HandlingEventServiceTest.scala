package se.citerus.dddsample.application

import java.util.Date

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import se.citerus.dddsample.application.impl.HandlingEventServiceImpl
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
  LOAD
}
import se.citerus.dddsample.domain.model.location.LocationRepository
import se.citerus.dddsample.domain.model.location.SampleLocations.*
import se.citerus.dddsample.domain.model.voyage.SampleVoyages.*
import se.citerus.dddsample.domain.model.voyage.VoyageRepository

class HandlingEventServiceTest extends AnyFunSuite with Matchers with MockitoSugar {

  private val cargo =
    new Cargo(new TrackingId("ABC"), new RouteSpecification(HAMBURG, TOKYO, new Date()))

  test("registerHandlingEvent stores event and notifies listeners") {
    val cargoRepository: CargoRepository                 = mock[CargoRepository]
    val voyageRepository: VoyageRepository               = mock[VoyageRepository]
    val handlingEventRepository: HandlingEventRepository = mock[HandlingEventRepository]
    val locationRepository: LocationRepository           = mock[LocationRepository]
    val applicationEvents: ApplicationEvents             = mock[ApplicationEvents]

    when(cargoRepository.find(cargo.trackingId)).thenReturn(Some(cargo))
    when(voyageRepository.find(CM001.voyageNumber)).thenReturn(Some(CM001))
    when(locationRepository.find(STOCKHOLM.unlocode)).thenReturn(Some(STOCKHOLM))

    val factory = new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository)
    val service = new HandlingEventServiceImpl(handlingEventRepository, applicationEvents, factory)

    service.registerHandlingEvent(
      new Date(),
      cargo.trackingId,
      CM001.voyageNumber,
      STOCKHOLM.unlocode,
      LOAD
    )

    verify(handlingEventRepository).store(any(classOf[HandlingEvent]))
    verify(applicationEvents).cargoWasHandled(any(classOf[HandlingEvent]))
  }

}
