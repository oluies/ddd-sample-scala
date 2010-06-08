package se.citerus.dddsample.application.impl

import org.apache.commons.logging.Log
import se.citerus.dddsample.domain.model.handling._;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import se.citerus.dddsample.application.ApplicationEvents;
import se.citerus.dddsample.application.HandlingEventService;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.handling.CannotCreateHandlingEventException

import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;

import java.util.Date;

class HandlingEventServiceImpl(val handlingEventRepository: HandlingEventRepository,
                               val applicationEvents: ApplicationEvents, 
                               handlingEventFactory:HandlingEventFactory) extends HandlingEventService {
  
  val logger = LogFactory.getLog(getClass());

  @Override
  //@Transactional(rollbackFor = new Array(CannotCreateHandlingEventException))
  def registerHandlingEvent(completionTime: Date,
                            trackingId: TrackingId,
                            voyageNumber: VoyageNumber,
                            unLocode: UnLocode,
                            eventType: HandlingEventType): Unit = {
    val registrationTime = new Date();
    /*
      Using a factory to create a HandlingEvent (aggregate). This is where
      it is determined whether the incoming data, the attempt, actually is capable
      of representing a real handling event. */
    val event = handlingEventFactory.createHandlingEvent(registrationTime, completionTime, trackingId, voyageNumber, unLocode, eventType);

    /* Store the new handling event, which updates the persistent
      state of the handling event aggregate (but not the cargo aggregate -
      that happens asynchronously!)
    */
    handlingEventRepository.store(event);

    /* Publish an event stating that a cargo has been handled. */
    applicationEvents.cargoWasHandled(event);

    logger.info("Registered handling event");
  }

}
