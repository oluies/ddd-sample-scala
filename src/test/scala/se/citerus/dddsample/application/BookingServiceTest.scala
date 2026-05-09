package se.citerus.dddsample.application

import java.util.Date

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import se.citerus.dddsample.application.impl.BookingServiceImpl
import se.citerus.dddsample.domain.model.cargo.{Cargo, CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.location.SampleLocations.*
import se.citerus.dddsample.domain.service.RoutingService

class BookingServiceTest extends AnyFunSuite with Matchers with MockitoSugar {

  test("bookNewCargo allocates a tracking id and stores the cargo") {
    val cargoRepository: CargoRepository       = mock[CargoRepository]
    val locationRepository: LocationRepository = mock[LocationRepository]
    val routingService: RoutingService         = mock[RoutingService]
    val bookingService = new BookingServiceImpl(cargoRepository, locationRepository, routingService)

    val expectedTrackingId = new TrackingId("TRK1")
    val fromUnlocode       = new UnLocode("USCHI")
    val toUnlocode         = new UnLocode("SESTO")

    when(cargoRepository.nextTrackingId()).thenReturn(expectedTrackingId)
    when(locationRepository.find(fromUnlocode)).thenReturn(Some(CHICAGO))
    when(locationRepository.find(toUnlocode)).thenReturn(Some(STOCKHOLM))

    val trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode, new Date())

    trackingId shouldEqual expectedTrackingId
    verify(cargoRepository).store(any(classOf[Cargo]))
  }

}
