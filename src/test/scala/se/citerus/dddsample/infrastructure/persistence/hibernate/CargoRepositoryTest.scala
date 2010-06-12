package se.citerus.dddsample.infrastructure.persistence.hibernate;

import org.junit._

import se.citerus.dddsample.application.util.SampleDataGenerator;
import se.citerus.dddsample.domain.model.cargo._;
import se.citerus.dddsample.domain.model.handling._;
import se.citerus.dddsample.domain.model.location._;
import se.citerus.dddsample.domain.model.location.SampleLocations._;

import se.citerus.dddsample.domain.model.voyage._;
import se.citerus.dddsample.domain.model.voyage.SampleVoyages._;

import java.util.Date

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.EasyMockSugar;


class CargoRepositoryTest extends AbstractRepositoryTest with AssertionsForJUnit {

  private var cargoRepository:CargoRepository = _
  private var locationRepository:LocationRepository = _
  private var voyageRepository:VoyageRepository = _
  
  private val sampleDataGenerator:SampleDataGenerator = new SampleDataGenerator()

  @Test
  def testFindByCargoId() : Unit = {
    val trackingId = new TrackingId("FGH");
    val cargo = cargoRepository.find(trackingId).getOrElse { throw new RuntimeException("Cannot get") }
    assert(STOCKHOLM === cargo.origin)
    assert(HONGKONG === cargo.routeSpecification.origin)
    assert(HELSINKI === cargo.routeSpecification.destination);

    assert(cargo.delivery != null);

    val handlingHistory = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId)
    val events = handlingHistory.distinctEventsByCompletionTime;
    assert(2 === events.size);

    val firstEvent = events(0);
    assertHandlingEvent(cargo, firstEvent, RECEIVE, HONGKONG, 100, 160, Voyage.NONE);

    val secondEvent = events(1);

    val hongkongMelbourneTokyoAndBack = new VoyageBuilder(
      new VoyageNumber("0303"), HONGKONG).
      addMovement(MELBOURNE, new Date(), new Date()).
      addMovement(TOKYO, new Date(), new Date()).
      addMovement(HONGKONG, new Date(), new Date()).
      build();
    
    assertHandlingEvent(cargo, secondEvent, LOAD, HONGKONG, 150, 110, hongkongMelbourneTokyoAndBack);

    val legs = cargo.itinerary.legs;
    assert(3 === legs.size);

    val firstLeg = legs(0);
    assertLeg(firstLeg, "0101", HONGKONG, MELBOURNE);

    val secondLeg = legs(1);
    assertLeg(secondLeg, "0101", MELBOURNE, STOCKHOLM);

    val thirdLeg = legs(2);
    assertLeg(thirdLeg, "0101", STOCKHOLM, HELSINKI);
  }

  private def assertHandlingEvent(cargo:Cargo, event:HandlingEvent, 
       expectedEventType:HandlingEventType, expectedLocation:Location, 
       completionTimeMs:Int, registrationTimeMs:Int, voyage:Voyage) : Unit = {
    assert(expectedEventType === event.eventType);
    assert(expectedLocation === event.location);

    val expectedCompletionTime = sampleDataGenerator.offset(completionTimeMs);
    assert(expectedCompletionTime === event.completionTime);

    val expectedRegistrationTime = sampleDataGenerator.offset(registrationTimeMs);
    assert(expectedRegistrationTime === event.registrationTime);

    assert(voyage === event.voyage);
    assert(cargo === event.cargo);
  }

  def testFindByCargoIdUnknownId() : Unit = {
    assert(cargoRepository.find(new TrackingId("UNKNOWN")) === None);
  }

  private def assertLeg(firstLeg:Leg, vn:String, expectedFrom:Location, expectedTo:Location) : Unit = {
    assert(new VoyageNumber(vn) === firstLeg.voyage.voyageNumber);
    assert(expectedFrom === firstLeg.loadLocation);
    assert(expectedTo === firstLeg.unloadLocation);
  }

  def testSave() : Unit = {
    val trackingId = new TrackingId("AAA");
    val origin = locationRepository.find(STOCKHOLM.unlocode).getOrElse { throw new RuntimeException("Cannot get") };
    val destination = locationRepository.find(MELBOURNE.unlocode).getOrElse { throw new RuntimeException("Cannot get") };

    val cargo = new Cargo(trackingId, new RouteSpecification(origin, destination, new Date()));
    cargoRepository.store(cargo);
    val voyage = voyageRepository.find(new VoyageNumber("0101")).getOrElse { throw new RuntimeException("Cannot get") }
    
    cargo.assignToRoute(new Itinerary(List(new Leg(voyage, origin, destination, new Date(), new Date()))));
    
    flush();

    import scala.collection.JavaConversions._    
    val map = sjt.queryForMap("select * from Cargo where tracking_id = ?", trackingId.idString);

    assert("AAA" === map("TRACKING_ID"));

    val originId = getLongId(origin);
    assert(originId === map("SPEC_ORIGIN_ID"));

    val destinationId = getLongId(destination);
    assert(destinationId === map("SPEC_DESTINATION_ID"));

    getSession().clear();

    val loadedCargo = cargoRepository.find(trackingId).getOrElse { throw new RuntimeException("not found") }
    assert(1 === loadedCargo.itinerary.legs.size);
  }

  def testReplaceItinerary() : Unit = {
    import scala.collection.JavaConversions._
    val cargo = cargoRepository.find(new TrackingId("FGH")).getOrElse { throw new RuntimeException("not found") };
    val cargoId = getLongId(cargo);
    val legCount = sjt.queryForInt("select count(*) from Leg where cargo_id = ?", cargoId.asInstanceOf[Object])
    assert(3 === legCount)

    val legFrom = locationRepository.find(new UnLocode("FIHEL")).getOrElse { throw new RuntimeException("not found") };
    val legTo = locationRepository.find(new UnLocode("DEHAM")).getOrElse { throw new RuntimeException("not found") };
    val newItinerary = new Itinerary(List(new Leg(CM004, legFrom, legTo, new Date(), new Date())));

    cargo.assignToRoute(newItinerary);

    cargoRepository.store(cargo);
    flush();

    val newLegCount = sjt.queryForInt("select count(*) from Leg where cargo_id = ?", cargoId.asInstanceOf[Object])
    assert(1 === newLegCount)
  }

  def testFindAll() : Unit = {
    val all = cargoRepository.findAll();
    assert(! all.isEmpty);
    assert(6 === all.size);
  }

  def testNextTrackingId() : Unit = {
    val trackingId = cargoRepository.nextTrackingId();
    assert(trackingId != null);

    val trackingId2 = cargoRepository.nextTrackingId();
    assert(trackingId2 != null);
    assert(! (trackingId equals trackingId2));
  }

}