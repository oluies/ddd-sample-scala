package se.citerus.dddsample.application.impl

import java.time.Instant

import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.cargo.{Cargo, CargoFactory, CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{Location, LocationRepository, UnLocode}
import se.citerus.dddsample.domain.service.RoutingService

class BookingServiceImplTest extends AnyFunSuite with Matchers:

  private val CHICAGO   = Location(UnLocode("USCHI"), "Chicago")
  private val STOCKHOLM = Location(UnLocode("SESTO"), "Stockholm")

  test("bookNewCargo stores cargo and returns its tracking id") {
    val cargoRepository    = mock(classOf[CargoRepository])
    val locationRepository = mock(classOf[LocationRepository])
    val routingService     = mock(classOf[RoutingService])
    val cargoFactory       = new CargoFactory(locationRepository, cargoRepository)
    val booking            = new BookingServiceImpl(cargoRepository, locationRepository, routingService, cargoFactory)

    val expectedTrackingId = TrackingId("TRK1")
    when(cargoRepository.nextTrackingId()).thenReturn(expectedTrackingId)
    when(locationRepository.find(UnLocode("USCHI"))).thenReturn(Some(CHICAGO))
    when(locationRepository.find(UnLocode("SESTO"))).thenReturn(Some(STOCKHOLM))

    val trackingId = booking.bookNewCargo(UnLocode("USCHI"), UnLocode("SESTO"), Instant.now())
    trackingId shouldEqual expectedTrackingId

    verify(cargoRepository, times(1)).store(isA(classOf[Cargo]))
    verify(locationRepository, times(1)).find(UnLocode("USCHI"))
    verify(locationRepository, times(1)).find(UnLocode("SESTO"))
  }
