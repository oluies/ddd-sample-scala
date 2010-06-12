package se.citerus.dddsample.interfaces.tracking;

import org.springframework.context.MessageSource;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.support.RequestContextUtils;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scala.reflect.BeanProperty

/**
 * Controller for tracking cargo. This interface sits immediately on top of the
 * domain layer, unlike the booking interface which has a a remote facade and supporting
 * DTOs in between.
 * <p/>
 * An adapter class, designed for the tracking use case, is used to wrap the domain model
 * to make it easier to work with in a web page rendering context. We do not want to apply
 * view rendering constraints to the design of our domain model, and the adapter
 * helps us shield the domain model classes. 
 * <p/>
 *
 * @see se.citerus.dddsample.application.web.CargoTrackingViewAdapter
 * @see se.citerus.dddsample.interfaces.booking.web.CargoAdminController
 *
 */
class CargoTrackingController extends SimpleFormController {

  @BeanProperty var cargoRepository:CargoRepository = _
  @BeanProperty var handlingEventRepository:HandlingEventRepository = _

  public CargoTrackingController() {
    setCommandClass(TrackCommand.class);
  }

  override
  protected onSubmit(request:HttpServletRequest, response:HttpServletResponse,
                      command:Object, errors:BindException) : ModelAndView = {

    val trackCommand = (TrackCommand) command;
    val trackingIdString = trackCommand.getTrackingId();

    val trackingId = new TrackingId(trackingIdString);
    val cargo = cargoRepository.find(trackingId);

    val model = Map[String, CargoTrackingViewAdapter]();
    if (cargo != null) {
      val messageSource = getApplicationContext();
      val locale = RequestContextUtils.getLocale(request);
      val handlingEvents = handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId).distinctEventsByCompletionTime();
      model.put("cargo", new CargoTrackingViewAdapter(cargo, messageSource, locale, handlingEvents));
    } else {
      errors.rejectValue("trackingId", "cargo.unknown_id", Array(trackCommand.trackingId), "Unknown tracking id");
    }
    return showForm(request, response, errors, model);
  }

}
