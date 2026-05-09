package se.citerus.dddsample.interfaces.handling

import java.io.Serializable
import java.util.Date

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

import se.citerus.dddsample.domain.model.cargo.TrackingId
import se.citerus.dddsample.domain.model.handling.HandlingEventType
import se.citerus.dddsample.domain.model.location.UnLocode
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;

/**
 * This is a simple transfer object for passing incoming handling event
 * registration attempts to proper the registration procedure.
 *
 * It is used as a message queue element.
 */
case class HandlingEventRegistrationAttempt(
    val registrationDate: Date,
    val completionDate: Date,
    val trackingId: TrackingId,
    val voyageNumber: VoyageNumber,
    val eventType: HandlingEventType,
    val unLocode: UnLocode
) extends Serializable {}
