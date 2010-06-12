package se.citerus.dddsample.infrastructure.persistence.hibernate

import org.hibernate.usertype.UserType
import org.hibernate._

import java.sql.Types
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet

abstract class HibernateUserType {

  private val SQL_TYPES : Array[Int] = { Array(Types.NUMERIC) };
    
  def sqlTypes() : Array[Int] = {
    return SQL_TYPES;
  }

  def returnedClass() : Class[_]

  def equals(x:Any, y:Any) : Boolean = {
    if (x == y) {
      return true;
    } else if (x == null || y == null) {
      return false;
    } else {
      return x.equals(y);
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