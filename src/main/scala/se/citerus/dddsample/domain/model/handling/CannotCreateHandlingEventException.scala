package se.citerus.dddsample.domain.model.handling

case class CannotCreateHandlingEventException(e:Exception) extends Exception(e) 