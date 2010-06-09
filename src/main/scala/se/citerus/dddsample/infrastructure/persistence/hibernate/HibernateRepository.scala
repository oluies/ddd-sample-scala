package se.citerus.dddsample.infrastructure.persistence.hibernate

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;

trait HibernateRepository {

  private var sessionFactory:SessionFactory = _
  
  @Required
  def setSessionFactory(newSessionFactory:SessionFactory) = {
    sessionFactory = newSessionFactory
  }
  
  protected def getSession():Session = {
    sessionFactory.getCurrentSession();
  }
}