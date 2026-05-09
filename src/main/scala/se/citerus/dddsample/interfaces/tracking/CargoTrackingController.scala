package se.citerus.dddsample.interfaces.tracking

import scala.beans.BeanProperty

import se.citerus.dddsample.domain.model.cargo.CargoRepository
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository

/**
 * Controller for tracking cargo.
 *
 * NOTE: The original implementation extended Spring 2.x's `SimpleFormController`,
 * which was removed in Spring 3.0. The controller logic needs to be rewritten
 * against the modern `@Controller` / `@RequestMapping` annotation model. This
 * stub keeps the class on the classpath so the WAR builds; the request-handling
 * logic is left as a TODO.
 *
 * @see se.citerus.dddsample.interfaces.booking.web.CargoAdminController
 */
class CargoTrackingController {

  @BeanProperty var cargoRepository: CargoRepository                 = _
  @BeanProperty var handlingEventRepository: HandlingEventRepository = _

  // TODO(scala3-migration): port from SimpleFormController to @Controller +
  // @GetMapping/@PostMapping. The original logic looked up a Cargo by tracking
  // id, wrapped it in a CargoTrackingViewAdapter, and rendered the form.
}
