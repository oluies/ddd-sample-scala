package se.citerus.dddsample.application

import junit.framework.TestCase
import scala.collection.mutable.ListBuffer
import junit.framework.Assert._
import org.scalatest.junit.AssertionsForJUnit

import org.easymock.EasyMock._
import se.citerus.dddsample.application.impl.BookingServiceImpl;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.location.LocationRepository;
//import static se.citerus.dddsample.domain.model.location.SampleLocations.CHICAGO;
//import static se.citerus.dddsample.domain.model.location.SampleLocations.STOCKHOLM;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.service.RoutingService;

class BookingServiceTest extends TestCase with AssertionsForJUnit with EasyMockSugar {

  var bookingService:BookingServiceImpl = _
  var cargoRepository:CargoRepository = _
  var locationRepository:LocationRepository = _
  var routingService:RoutingService = _

  override def setUp() = {
    cargoRepository = createMock(CargoRepository.getClass());
    locationRepository = createMock(LocationRepository.getClass());
    routingService = createMock(RoutingService.getClass());
    bookingService = new BookingServiceImpl(cargoRepository, locationRepository, routingService);
  }

  override def tearDown() = {
    verify(cargoRepository, locationRepository);
  }

  def testRegisterNew() = {
    val expectedTrackingId = new TrackingId("TRK1");
    val fromUnlocode = new UnLocode("USCHI");
    val toUnlocode = new UnLocode("SESTO");

    expect(cargoRepository.nextTrackingId()).andReturn(expectedTrackingId);
    expect(locationRepository.find(fromUnlocode)).andReturn(CHICAGO);
    expect(locationRepository.find(toUnlocode)).andReturn(STOCKHOLM);

    cargoRepository.store(isA(Cargo.getClass()));

    replay(cargoRepository, locationRepository);

    val trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode, new Date());
    assertEquals(expectedTrackingId, trackingId);
  }

}