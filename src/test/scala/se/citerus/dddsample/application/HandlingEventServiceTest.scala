package se.citerus.dddsample.application;

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.EasyMockSugar;

import se.citerus.dddsample.application.impl.HandlingEventServiceImpl;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.RouteSpecification;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingEventFactory;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.SampleLocations._;
import se.citerus.dddsample.domain.model.voyage.SampleVoyages._;
import se.citerus.dddsample.domain.model.handling._;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;

import java.util.Date;

class HandlingEventServiceTest extends TestCase with AssertionsForJUnit with EasyMockSugar {
  var service:HandlingEventServiceImpl = _
  var applicationEvents:ApplicationEvents = _
  var cargoRepository:CargoRepository = _
  var voyageRepository:VoyageRepository = _
  var handlingEventRepository:HandlingEventRepository = _
  var locationRepository:LocationRepository = _

  private val cargo = new Cargo(new TrackingId("ABC"), new RouteSpecification(HAMBURG, TOKYO, new Date()));

  override def setUp() = {    
    cargoRepository = mock[CargoRepository]
    voyageRepository = mock[VoyageRepository]
    handlingEventRepository = mock[HandlingEventRepository]
    locationRepository = mock[LocationRepository]
    applicationEvents = mock[ApplicationEvents]

    val handlingEventFactory = new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository);
    service = new HandlingEventServiceImpl(handlingEventRepository, applicationEvents, handlingEventFactory);
  }

  override def tearDown() = {
    verify(cargoRepository, voyageRepository, handlingEventRepository, applicationEvents);
  }

  def testRegisterEvent() = {
    expecting {
      call(cargoRepository.find(cargo.trackingId)).andReturn(Some(cargo));
      call(voyageRepository.find(CM001.voyageNumber)).andReturn(Some(CM001));
      call(locationRepository.find(STOCKHOLM.unlocode)).andReturn(Some(STOCKHOLM));
    }
    
    handlingEventRepository.store(isA(classOf[HandlingEvent]));
    applicationEvents.cargoWasHandled(isA(classOf[HandlingEvent]));

    replay(cargoRepository, voyageRepository, handlingEventRepository, locationRepository, applicationEvents);

    service.registerHandlingEvent(new Date(), cargo.trackingId, CM001.voyageNumber, STOCKHOLM.unlocode, LOAD);
  }

}
