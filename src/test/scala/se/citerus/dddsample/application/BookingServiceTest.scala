package se.citerus.dddsample.application

import scala.collection.mutable.ListBuffer

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.EasyMockSugar;

import se.citerus.dddsample.application.impl.BookingServiceImpl;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.service.RoutingService;

import se.citerus.dddsample.domain.model.location.SampleLocations._

import java.util.Date

class BookingServiceTest extends TestCase with AssertionsForJUnit with EasyMockSugar {

  var bookingService:BookingServiceImpl = _
  var cargoRepository:CargoRepository = _
  var locationRepository:LocationRepository = _
  var routingService:RoutingService = _

  override def setUp() = {
    cargoRepository = mock[CargoRepository]
    locationRepository = mock[LocationRepository]
    routingService = mock[RoutingService];
    bookingService = new BookingServiceImpl(cargoRepository, locationRepository, routingService);
  }

  override def tearDown() = {
    verify(cargoRepository, locationRepository);
  }

  def testRegisterNew() = {
    val expectedTrackingId = new TrackingId("TRK1");
    val fromUnlocode = new UnLocode("USCHI");
    val toUnlocode = new UnLocode("SESTO");

    expecting {
      call(cargoRepository.nextTrackingId()).andReturn(expectedTrackingId)
      call(locationRepository.find(fromUnlocode)).andReturn(Some(CHICAGO))
      call(locationRepository.find(toUnlocode)).andReturn(Some(STOCKHOLM))
    }
    
    cargoRepository.store(isA(classOf[Cargo]));

    replay(cargoRepository, locationRepository);

    val trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode, new Date());
    assertEquals(expectedTrackingId, trackingId);
  }

}