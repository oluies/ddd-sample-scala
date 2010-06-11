package se.citerus.dddsample.infrastructure.persistence.hibernate;

import se.citerus.dddsample.application.util.SampleDataGenerator;
import se.citerus.dddsample.domain.model.cargo._;
import se.citerus.dddsample.domain.model.handling._;
import se.citerus.dddsample.domain.model.location._;
import se.citerus.dddsample.domain.model.voyage._;

import java.util.Date

class CargoRepositoryTest extends AbstractRepositoryTest {

  private var cargoRepository:CargoRepository = _
  private var locationRepository:LocationRepository = _
  private var voyageRepository:VoyageRepository = _
  
  private val sampleDataGenerator:SampleDataGenerator = new SampleDataGenerator()

  def testFindByCargoId() : Unit = {
    val trackingId = new TrackingId("FGH");
    val cargo = cargoRepository.find(trackingId).getOrElse { throw new RuntimeException("Cannot get") };
    assertEquals(STOCKHOLM, cargo.origin());
    assertEquals(HONGKONG, cargo.routeSpecification().origin());
    assertEquals(HELSINKI, cargo.routeSpecification().destination());

    assertNotNull(cargo.delivery());

    val handlingHistory = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId)
    val events = handlingHistory.distinctEventsByCompletionTime;
    assertEquals(2, events.size());

    val firstEvent = events.get(0);
    assertHandlingEvent(cargo, firstEvent, RECEIVE, HONGKONG, 100, 160, Voyage.NONE);

    val secondEvent = events.get(1);

    val hongkongMelbourneTokyoAndBack = new VoyageBuilder(
      new VoyageNumber("0303"), HONGKONG).
      addMovement(MELBOURNE, new Date(), new Date()).
      addMovement(TOKYO, new Date(), new Date()).
      addMovement(HONGKONG, new Date(), new Date()).
      build();
    
    assertHandlingEvent(cargo, secondEvent, LOAD, HONGKONG, 150, 110, hongkongMelbourneTokyoAndBack);

    val legs = cargo.itinerary.legs;
    assertEquals(3, legs.size());

    val firstLeg = legs(0);
    assertLeg(firstLeg, "0101", HONGKONG, MELBOURNE);

    val secondLeg = legs(1);
    assertLeg(secondLeg, "0101", MELBOURNE, STOCKHOLM);

    val thirdLeg = legs.get(2);
    assertLeg(thirdLeg, "0101", STOCKHOLM, HELSINKI);
  }

  private def assertHandlingEvent(cargo:Cargo, event:HandlingEvent, 
       expectedEventType:HandlingEventType, expectedLocation:Location, 
       completionTimeMs:Int, registrationTimeMs:Int, voyage:Voyage) : Unit = {
    assertEquals(expectedEventType, event.eventType);
    assertEquals(expectedLocation, event.location());

    val expectedCompletionTime = sampleDataGenerator.offset(completionTimeMs);
    assertEquals(expectedCompletionTime, event.completionTime());

    val expectedRegistrationTime = sampleDataGenerator.offset(registrationTimeMs);
    assertEquals(expectedRegistrationTime, event.registrationTime());

    assertEquals(voyage, event.voyage());
    assertEquals(cargo, event.cargo());
  }

  def testFindByCargoIdUnknownId() : Unit = {
    assertNull(cargoRepository.find(new TrackingId("UNKNOWN")));
  }

  private def assertLeg(firstLeg:Leg, vn:String, expectedFrom:Location, expectedTo:Location) : Unit = {
    assertEquals(new VoyageNumber(vn), firstLeg.voyage().voyageNumber());
    assertEquals(expectedFrom, firstLeg.loadLocation());
    assertEquals(expectedTo, firstLeg.unloadLocation());
  }

  def testSave() : Unit = {
    val trackingId = new TrackingId("AAA");
    val origin = locationRepository.find(STOCKHOLM.unLocode());
    val destination = locationRepository.find(MELBOURNE.unLocode());

    val cargo = new Cargo(trackingId, new RouteSpecification(origin, destination, new Date()));
    cargoRepository.store(cargo);

    cargo.assignToRoute(new Itinerary(List(
      new Leg(
        voyageRepository.find(new VoyageNumber("0101")),
        locationRepository.find(STOCKHOLM.unLocode()),
        locationRepository.find(MELBOURNE.unLocode()),
        new Date(), new Date())
    )));
    
    flush();

    val map:Map[String, Object] = sjt.queryForMap(
      "select * from Cargo where tracking_id = ?", trackingId.idString);

    assertEquals("AAA", map.get("TRACKING_ID"));

    val originId = getLongId(origin);
    assertEquals(originId, map.get("SPEC_ORIGIN_ID"));

    val destinationId = getLongId(destination);
    assertEquals(destinationId, map.get("SPEC_DESTINATION_ID"));

    getSession().clear();

    val loadedCargo = cargoRepository.find(trackingId);
    assertEquals(1, loadedCargo.itinerary().legs().size());
  }

  def testReplaceItinerary() : Unit = {
    val cargo = cargoRepository.find(new TrackingId("FGH")).getOrElse { throw new RuntimeException("not found") };
    val cargoId = getLongId(cargo);
    assertEquals(3, sjt.queryForInt("select count(*) from Leg where cargo_id = ?", cargoId));

    val legFrom = locationRepository.find(new UnLocode("FIHEL"));
    val legTo = locationRepository.find(new UnLocode("DEHAM"));
    val newItinerary = new Itinerary(List(new Leg(CM004, legFrom, legTo, new Date(), new Date())));

    cargo.assignToRoute(newItinerary);

    cargoRepository.store(cargo);
    flush();

    assertEquals(1, sjt.queryForInt("select count(*) from Leg where cargo_id = ?", cargoId));
  }

  def testFindAll() : Unit = {
    val all = cargoRepository.findAll();
    assertNotNull(all);
    assertEquals(6, all.size());
  }

  def testNextTrackingId() : Unit = {
    val trackingId = cargoRepository.nextTrackingId();
    assertNotNull(trackingId);

    val trackingId2 = cargoRepository.nextTrackingId();
    assertNotNull(trackingId2);
    assertFalse(trackingId.equals(trackingId2));
  }

}