package se.citerus.dddsample.interfaces.booking.web

import scala.beans.BeanProperty

/** Request body for `POST /admin/cargos` — origin, destination, deadline. */
final class RegistrationCommand:
  @BeanProperty var originUnlocode: String      = ""
  @BeanProperty var destinationUnlocode: String = ""
  @BeanProperty var arrivalDeadline: String     = ""
