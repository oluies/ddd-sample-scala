package se.citerus.dddsample.application.util

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import se.citerus.dddsample.domain.model.cargo._;
import se.citerus.dddsample.domain.model.handling._;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.SampleLocations;
import se.citerus.dddsample.domain.model.location.SampleLocations._;
import se.citerus.dddsample.domain.model.voyage.SampleVoyages._;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class SampleDataGenerator extends ServletContextListener {

  private val base = new Timestamp(new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime() - 1000L * 60 * 60 * 24 * 100)
  
  private def loadHandlingEventData(jdbcTemplate:JdbcTemplate) : Unit = {
    val handlingEventSql =
      "insert into HandlingEvent (completionTime, registrationTime, type, location_id, voyage_id, cargo_id) " +
      "values (?, ?, ?, ?, ?, ?)";

     val handlingEventArgs = Array(
        //XYZ (SESTO-FIHEL-DEHAM-CNHKG-JPTOK-AUMEL)
        Array(ts(0),     ts((0)),    "RECEIVE",  1,  null,  1),
        Array(ts((4)),   ts((5)),    "LOAD",     1,  1,     1),
        Array(ts((14)),  ts((14)),   "UNLOAD",   5,  1,     1),
        Array(ts((15)),  ts((15)),   "LOAD",     5,  1,     1),
        Array(ts((30)),  ts((30)),   "UNLOAD",   6,  1,     1),
        Array(ts((33)),  ts((33)),   "LOAD",     6,  1,     1),
        Array(ts((34)),  ts((34)),   "UNLOAD",   3,  1,     1),
        Array(ts((60)),  ts((60)),   "LOAD",     3,  1,     1),
        Array(ts((70)),  ts((71)),   "UNLOAD",   4,  1,     1),
        Array(ts((75)),  ts((75)),   "LOAD",     4,  1,     1),
        Array(ts((88)),  ts((88)),   "UNLOAD",   2,  1,     1),
        Array(ts((100)), ts((102)),  "CLAIM",    2,  null,  1),

        //ZYX (AUMEL - USCHI - DEHAM -)
        Array(ts((200)),   ts((201)),  "RECEIVE",  2,  null,  3),
        Array(ts((202)),   ts((202)),  "LOAD",     2,  2,     3),
        Array(ts((208)),   ts((208)),  "UNLOAD",   7,  2,     3),
        Array(ts((212)),   ts((212)),  "LOAD",     7,  2,     3),
        Array(ts((230)),   ts((230)),  "UNLOAD",   6,  2,     3),
        Array(ts((235)),   ts((235)),  "LOAD",     6,  2,     3),

        //ABC
        Array(ts((20)),  ts((21)),   "CLAIM",    2,  null,  2),

        //CBA
        Array(ts((0)),   ts((1)),    "RECEIVE",  2,  null,  4),
        Array(ts((10)),  ts((11)),   "LOAD",     2,  2,     4),
        Array(ts((20)),  ts((21)),   "UNLOAD",   7,  2,     4),

        //FGH
        Array(ts(100),   ts(160),    "RECEIVE",  3,  null,   5),
        Array(ts(150),   ts(110),    "LOAD",     3,  3,     5),

        // JKL
        Array(ts(200),   ts(220),    "RECEIVE",  6,  null,   6),
        Array(ts(300),   ts(330),    "LOAD",     6,  3,     6),
        Array(ts(400),   ts(440),    "UNLOAD",   5,  3,     6)  // Unexpected event
    );
    executeUpdate(jdbcTemplate, handlingEventSql, handlingEventArgs);
  }

  private def loadCarrierMovementData(jdbcTemplate:JdbcTemplate) = {
    val voyageSql = "insert into Voyage (id, voyage_number) values (?, ?)";
    val voyageArgs = Array(
      Array(1,"0101"),
      Array(2,"0202"),
      Array(3,"0303")
    );
    executeUpdate(jdbcTemplate, voyageSql, voyageArgs);

    val carrierMovementSql =
      "insert into CarrierMovement (id, voyage_id, departure_location_id, arrival_location_id, departure_time, arrival_time, cm_index) " +
      "values (?,?,?,?,?,?,?)";

    val carrierMovementArgs = Array(
      // SESTO - FIHEL - DEHAM - CNHKG - JPTOK - AUMEL (voyage 0101)
      Array(1,1,1,5,ts(1),ts(2),0),
      Array(2,1,5,6,ts(1),ts(2),1),
      Array(3,1,6,3,ts(1),ts(2),2),
      Array(4,1,3,4,ts(1),ts(2),3),
      Array(5,1,4,2,ts(1),ts(2),4),

      // AUMEL - USCHI - DEHAM - SESTO - FIHEL (voyage 0202)
      Array(7,2,2,7,ts(1),ts(2),0),
      Array(8,2,7,6,ts(1),ts(2),1),
      Array(9,2,6,1,ts(1),ts(2),2),
      Array(6,2,1,5,ts(1),ts(2),3),

      // CNHKG - AUMEL - FIHEL - DEHAM - SESTO - USCHI - JPTKO (voyage 0303)
      Array(10,3,3,2,ts(1),ts(2),0),
      Array(11,3,2,5,ts(1),ts(2),1),
      Array(12,3,6,1,ts(1),ts(2),2),
      Array(13,3,1,7,ts(1),ts(2),3),
      Array(14,3,7,4,ts(1),ts(2),4)
    );
    executeUpdate(jdbcTemplate, carrierMovementSql, carrierMovementArgs);
  }

  private def loadCargoData(jdbcTemplate:JdbcTemplate) = {
    val cargoSql =
      "insert into Cargo (id, tracking_id, origin_id, spec_origin_id, spec_destination_id, spec_arrival_deadline, transport_status, current_voyage_id, last_known_location_id, is_misdirected, routing_status, calculated_at, unloaded_at_dest) " +
      "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    val cargoArgs = Array(
      Array(1, "XYZ", 1, 1, 2, ts(10), "IN_PORT", null, 1, false, "ROUTED", ts(100), false),
      Array(2, "ABC", 1, 1, 5, ts(20), "IN_PORT", null, 1, false, "ROUTED", ts(100), false),
      Array(3, "ZYX", 2, 2, 1, ts(30), "IN_PORT", null, 1, false, "NOT_ROUTED", ts(100), false),
      Array(4, "CBA", 5, 5, 1, ts(40), "IN_PORT", null, 1, false, "MISROUTED", ts(100), false),
      Array(5, "FGH", 1, 3, 5, ts(50), "IN_PORT", null, 1, false, "ROUTED", ts(100), false),  // Cargo origin differs from spec origin
      Array(6, "JKL", 6, 6, 4, ts(60), "IN_PORT", null, 1, true, "ROUTED", ts(100), false)
    );
    executeUpdate(jdbcTemplate, cargoSql, cargoArgs);
  }

  private def loadLocationData(jdbcTemplate:JdbcTemplate) = {
    val locationSql =
      "insert into Location (id, unlocode, name) " +
      "values (?, ?, ?)";

    val locationArgs = Array(
      Array(1, "SESTO", "Stockholm"),
      Array(2, "AUMEL", "Melbourne"),
      Array(3, "CNHKG", "Hongkong"),
      Array(4, "JPTOK", "Tokyo"),
      Array(5, "FIHEL", "Helsinki"),
      Array(6, "DEHAM", "Hamburg"),
      Array(7, "USCHI", "Chicago")
    );
    executeUpdate(jdbcTemplate, locationSql, locationArgs);
  }

  private def loadItineraryData(jdbcTemplate:JdbcTemplate) = {
    val legSql =
      "insert into Leg (id, cargo_id, voyage_id, load_location_id, unload_location_id, load_time, unload_time, leg_index) " +
      "values (?,?,?,?,?,?,?,?)";

    val legArgs = Array(
      // Cargo 5: Hongkong - Melbourne - Stockholm - Helsinki
      Array(1,5,1,3,2,ts(1),ts(2),0),
      Array(2,5,1,2,1,ts(3),ts(4),1),
      Array(3,5,1,1,5,ts(4),ts(5),2),
      // Cargo 6: Hamburg - Stockholm - Chicago - Tokyo
      Array(4,6,2,6,1,ts(1),ts(2),0),
      Array(5,6,2,1,7,ts(3),ts(4),1),
      Array(6,6,2,7,4,ts(5),ts(6),2)
    );
    executeUpdate(jdbcTemplate, legSql, legArgs);
  }

  def contextInitialized(event:ServletContextEvent) : Unit = {
    val context = WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext());
    val dataSource = getBean(context, classOf[DataSource]);
    val transactionManager = getBean(context, classOf[PlatformTransactionManager]);
    val tt = new TransactionTemplate(transactionManager);
    //loadSampleData(new JdbcTemplate(dataSource), tt);

    val sf = getBean(context, classOf[SessionFactory]);
    val handlingEventFactory = new HandlingEventFactory(
      getBean(context, classOf[CargoRepository]),
      getBean(context, classOf[VoyageRepository]),
      getBean(context, classOf[LocationRepository]));
    loadHibernateData(tt, sf, handlingEventFactory, getBean(context, classOf[HandlingEventRepository]));
  }

  private def getBean[T](context:WebApplicationContext, cls:Class[T]) : T = {
    return BeanFactoryUtils.beanOfType(context, cls).asInstanceOf[T];
  }

  def loadHibernateData(tt:TransactionTemplate, sf:SessionFactory, 
        handlingEventFactory:HandlingEventFactory, handlingEventRepository:HandlingEventRepository) : Unit = {
    System.out.println("*** Loading Hibernate data ***");
    tt.execute(new TransactionCallbackWithoutResult() {
      override protected def doInTransactionWithoutResult(status:TransactionStatus) : Unit = {
        val session = sf.getCurrentSession();

        for (location <- SampleLocations.getAll()) {
          session.save(location);
        }

        session.save(HONGKONG_TO_NEW_YORK);
        session.save(NEW_YORK_TO_DALLAS);
        session.save(DALLAS_TO_HELSINKI);
        session.save(HELSINKI_TO_HONGKONG);
        session.save(DALLAS_TO_HELSINKI_ALT);

        val routeSpecification = new RouteSpecification(HONGKONG, HELSINKI, toDate("2009-03-15"));
        val trackingId = new TrackingId("ABC123");
        val abc123 = new Cargo(trackingId, routeSpecification);

        val itinerary = Itinerary(List(
          new Leg(HONGKONG_TO_NEW_YORK, HONGKONG, NEWYORK, toDate("2009-03-02"), toDate("2009-03-05")),
          new Leg(NEW_YORK_TO_DALLAS, NEWYORK, DALLAS, toDate("2009-03-06"), toDate("2009-03-08")),
          new Leg(DALLAS_TO_HELSINKI, DALLAS, HELSINKI, toDate("2009-03-09"), toDate("2009-03-12"))
        ));
        abc123.assignToRoute(itinerary);

        session.save(abc123);
        
        {
          val event1 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-01"), trackingId, null, HONGKONG.unlocode, RECEIVE
          );
          session.save(event1);
  
          val event2 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-02"), trackingId, HONGKONG_TO_NEW_YORK.voyageNumber, HONGKONG.unlocode, LOAD
          );
          session.save(event2);
  
          val event3 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-05"), trackingId, HONGKONG_TO_NEW_YORK.voyageNumber, NEWYORK.unlocode, UNLOAD
          );
          session.save(event3);
        }
        
        val handlingHistory = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId);
        abc123.deriveDeliveryProgress(handlingHistory);

        session.update(abc123);

        // Cargo JKL567

        val routeSpecification1 = new RouteSpecification(HANGZHOU, STOCKHOLM, toDate("2009-03-18"));
        val trackingId1 = new TrackingId("JKL567");
        val jkl567 = new Cargo(trackingId1, routeSpecification1);

        val itinerary1 = Itinerary(List(
          new Leg(HONGKONG_TO_NEW_YORK, HANGZHOU, NEWYORK, toDate("2009-03-03"), toDate("2009-03-05")),
          new Leg(NEW_YORK_TO_DALLAS, NEWYORK, DALLAS, toDate("2009-03-06"), toDate("2009-03-08")),
          new Leg(DALLAS_TO_HELSINKI, DALLAS, STOCKHOLM, toDate("2009-03-09"), toDate("2009-03-11"))
        ));
        jkl567.assignToRoute(itinerary1);

        session.save(jkl567);
        {
          val event1 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-01"), trackingId1, null, HANGZHOU.unlocode, RECEIVE
          );
          session.save(event1);
  
          val event2 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-03"), trackingId1, HONGKONG_TO_NEW_YORK.voyageNumber, HANGZHOU.unlocode, LOAD
          );
          session.save(event2);
  
          val event3 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-05"), trackingId1, HONGKONG_TO_NEW_YORK.voyageNumber, NEWYORK.unlocode, UNLOAD
          );
          session.save(event3);
  
          val event4 = handlingEventFactory.createHandlingEvent(
            new Date(), toDate("2009-03-06"), trackingId1, HONGKONG_TO_NEW_YORK.voyageNumber, NEWYORK.unlocode, LOAD
          );
          session.save(event4); 
        }

        val handlingHistory1 = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId1);
        jkl567.deriveDeliveryProgress(handlingHistory1);

        session.update(jkl567);
      }
    });
  }

  def contextDestroyed(event:ServletContextEvent) : Unit = {}

  def loadSampleData(jdbcTemplate:JdbcTemplate, transactionTemplate:TransactionTemplate) : Unit = {
    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
      protected def doInTransactionWithoutResult(status:TransactionStatus) : Unit = {
        loadLocationData(jdbcTemplate);
        loadCarrierMovementData(jdbcTemplate);
        loadCargoData(jdbcTemplate);
        loadItineraryData(jdbcTemplate);
        loadHandlingEventData(jdbcTemplate);
      }
    });
  }

  private def executeUpdate(jdbcTemplate:JdbcTemplate, sql:String, args:Array[Array[Any]]) : Unit = {
    for (arg <- args) {
      jdbcTemplate.update(sql, arg.asInstanceOf[Array[Object]]);
    }
  }

  private def ts(hours:Int) : Timestamp = {
    return new Timestamp(base.getTime() + 1000L * 60 * 60 * hours);
  }

  def offset(hours:Int) : Date = {
    return new Date(ts(hours).getTime());
  }
}