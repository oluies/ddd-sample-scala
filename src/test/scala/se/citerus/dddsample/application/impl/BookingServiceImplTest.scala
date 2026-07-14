package se.citerus.dddsample.application.impl

import java.time.Instant

import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.{mock, times, verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import se.citerus.dddsample.domain.model.cargo.{Cargo, CargoFactory, CargoRepository, TrackingId}
import se.citerus.dddsample.domain.model.location.{Location, LocationRepository, UnLocode}
import se.citerus.dddsample.domain.service.RoutingService
import se.citerus.dddsample.domain.shared.DomainError

class BookingServiceImplTest extends AnyFunSuite with Matchers:

  private val CHICAGO   = Location(UnLocode("USCHI"), "Chicago")
  private val STOCKHOLM = Location(UnLocode("SESTO"), "Stockholm")

  test("bookNewCargo stores cargo and returns Right(trackingId)") {
    val cargoRepository    = mock(classOf[CargoRepository])
    val locationRepository = mock(classOf[LocationRepository])
    val routingService     = mock(classOf[RoutingService])
    val cargoFactory       = new CargoFactory(locationRepository, cargoRepository)
    val booking =
      new BookingServiceImpl(cargoRepository, locationRepository, routingService, cargoFactory)

    val expectedTrackingId = TrackingId("TRK1")
    when(cargoRepository.nextTrackingId()).thenReturn(expectedTrackingId)
    when(locationRepository.find(UnLocode("USCHI"))).thenReturn(Some(CHICAGO))
    when(locationRepository.find(UnLocode("SESTO"))).thenReturn(Some(STOCKHOLM))

    val result = booking.bookNewCargo(UnLocode("USCHI"), UnLocode("SESTO"), Instant.now())
    result shouldEqual Right(expectedTrackingId)

    verify(cargoRepository, times(1)).store(isA(classOf[Cargo]))
    verify(locationRepository, times(1)).find(UnLocode("USCHI"))
    verify(locationRepository, times(1)).find(UnLocode("SESTO"))
  }

  test("bookNewCargo with unknown origin returns Left(UnknownLocation)") {
    val cargoRepository    = mock(classOf[CargoRepository])
    val locationRepository = mock(classOf[LocationRepository])
    val routingService     = mock(classOf[RoutingService])
    val cargoFactory       = new CargoFactory(locationRepository, cargoRepository)
    val booking =
      new BookingServiceImpl(cargoRepository, locationRepository, routingService, cargoFactory)

    when(locationRepository.find(UnLocode("XXAAA"))).thenReturn(None)

    val result = booking.bookNewCargo(UnLocode("XXAAA"), UnLocode("SESTO"), Instant.now())
    result shouldEqual Left(DomainError.UnknownLocation(UnLocode("XXAAA")))
  }

  test("assignCargoToRoute with unknown cargo returns Left(UnknownCargo)") {
    val cargoRepository    = mock(classOf[CargoRepository])
    val locationRepository = mock(classOf[LocationRepository])
    val routingService     = mock(classOf[RoutingService])
    val cargoFactory       = new CargoFactory(locationRepository, cargoRepository)
    val booking =
      new BookingServiceImpl(cargoRepository, locationRepository, routingService, cargoFactory)

    val unknownId = TrackingId("MISSING")
    when(cargoRepository.find(unknownId)).thenReturn(None)

    val itinerary = null.asInstanceOf[se.citerus.dddsample.domain.model.cargo.Itinerary]
    val result    = booking.assignCargoToRoute(itinerary, unknownId)
    result shouldEqual Left(DomainError.UnknownCargo(unknownId))
  }
