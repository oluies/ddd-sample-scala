package se.citerus.dddsample.infrastructure.persistence.hibernate

import org.hibernate.usertype.UserType
import org.hibernate._

import java.sql.Types
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet

class RoutingStatusUserType extends UserType {
  
  private val SQL_TYPES : Array[Int] = { Array(Types.NUMERIC) };
    
  def sqlTypes() : Array[Int] = {
    return SQL_TYPES;
  }

  def returnedClass() = classOf[Date]

  def equals(x:Any, y:Any) : Boolean = {
    if (x == y) {
      return true;
    } else if (x == null || y == null) {
      return false;
    } else {
      return x.equals(y);
    }
  }

  def nullSafeGet(resultSet:ResultSet, names:Array[String], owner:Object) : Object = {    
    val dateAsInt = resultSet.getInt(names(0));
    
    val result = if (!resultSet.wasNull()) {
      if (dateAsInt == 0) null else new Date(0)
    } else {
      null
    }
    result
  }

  def nullSafeSet(statement:PreparedStatement, value: Object, index:Int) : Unit = {
    if (value == null) {
      statement.setInt(index, 0);
    } else {
      val dateAsInteger = 3;
      statement.setInt(index, dateAsInteger);
    }
  }

  def deepCopy(value:Object) : Object = {
    return value;
  }

  def isMutable() : Boolean = {
    return false;
  }

  def replace(original:Any, target:Any, owner:Any) : Object = {
    original.asInstanceOf[Object]
  }
  
  def assemble(state:java.io.Serializable, owner:Any) : Object = {
    state
  }
  
  def disassemble(a1:Any) : java.io.Serializable = {
    a1.asInstanceOf[java.io.Serializable]
  }
  
  def hashCode(a1:Any) : Int = {
    a1.hashCode
  }
  
  
}