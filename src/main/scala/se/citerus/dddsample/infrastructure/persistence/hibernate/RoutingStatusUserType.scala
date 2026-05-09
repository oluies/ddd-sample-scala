package se.citerus.dddsample.infrastructure.persistence.hibernate

import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

import org.hibernate._
import org.hibernate.usertype.UserType

import se.citerus.dddsample.domain.model.cargo._

class RoutingStatusUserType extends HibernateUserType {

  override def returnedClass() = classOf[RoutingStatus]

  def nullSafeGet(resultSet: ResultSet, names: Array[String], owner: Object): Object = {
    if (resultSet.wasNull()) {
      return null
    }

    val valueAsInt = resultSet.getInt(names(0));
    valueAsInt match {
      case 1 => NOT_ROUTED
      case 2 => ROUTED
      case 3 => MISROUTED
      case _ => null
    }
  }

  def nullSafeSet(statement: PreparedStatement, value: Object, index: Int): Unit =
    if (value == null) {
      statement.setInt(index, 0);
    } else {
      val valueAsInt = value match {
        case NOT_ROUTED => 1
        case ROUTED     => 2
        case MISROUTED  => 3
      }
      statement.setInt(index, valueAsInt);
    }

}
