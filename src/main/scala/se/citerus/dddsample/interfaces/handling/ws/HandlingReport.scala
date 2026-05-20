package se.citerus.dddsample.interfaces.handling.ws

import java.time.LocalDateTime
import java.util.{List as JList}
import scala.beans.BeanProperty

import com.fasterxml.jackson.annotation.JsonProperty

/** Request body for `POST /handlingReport` — an attempt to register one or
  * more handling events for a list of cargo tracking ids.
  *
  * Mutable Java-bean shape (BeanProperty + JsonProperty) so Jackson can
  * deserialize it directly into the field set the upstream service expects.
  */
final class HandlingReport:
  @JsonProperty(required = true) @BeanProperty var completionTime: LocalDateTime = null
  @JsonProperty(required = true) @BeanProperty var trackingIds: JList[String]    = null
  @JsonProperty(required = true) @BeanProperty var `type`: String                = null
  @JsonProperty(required = true) @BeanProperty var unLocode: String              = null
                                  @BeanProperty var voyageNumber: String         = null
