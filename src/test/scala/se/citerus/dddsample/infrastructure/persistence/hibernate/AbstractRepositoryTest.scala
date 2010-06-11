package se.citerus.dddsample.infrastructure.persistence.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

import se.citerus.dddsample.application.util.SampleDataGenerator;
import se.citerus.dddsample.domain.model.handling.HandlingEventFactory;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory._;

import java.lang.reflect.Field;

class AbstractRepositoryTest extends AbstractTransactionalDataSourceSpringContextTests {

  setAutowireMode(AUTOWIRE_BY_NAME);
  setDependencyCheck(false);

  var sessionFactory:SessionFactory = _
  var sjt:SimpleJdbcTemplate = _
  var handlingEventFactory:HandlingEventFactory = _
  var handlingEventRepository:HandlingEventRepository = _

  def setSessionFactory(sessionFactory:SessionFactory) = {
    this.sessionFactory = sessionFactory;
    transactionManager = new HibernateTransactionManager(sessionFactory);
  }

   def getSessionFactory() : SessionFactory = {
     sessionFactory;
  }

  def flush() = {
    sessionFactory.getCurrentSession().flush();
  }

  override
  protected def getConfigLocations() : Array[String] = {
    Array("/context-infrastructure-persistence.xml", "context-domain.xml")
  }

  override
  protected def onSetUpInTransaction() : Unit = {
    // TODO store Sample* and object instances here instead of handwritten SQL
    val dataGenerator = new SampleDataGenerator()
    dataGenerator.loadSampleData(jdbcTemplate, new TransactionTemplate(transactionManager));
    sjt = new SimpleJdbcTemplate(jdbcTemplate);
  }

  protected def getSession() : Session = {
    return sessionFactory.getCurrentSession();
  }

  // Instead of exposing a getId() on persistent classes
  protected def getLongId(o:Any) : Long = {
    if (getSession().contains(o)) {
      return getSession().getIdentifier(o).asInstanceOf[Long];
    } else {
      try {
        var id:Field = o.asInstanceOf[AnyRef].getClass().getDeclaredField("id");
        id.setAccessible(true);
        return id.get(o).asInstanceOf[Long];
      } catch {
        case e:Exception => throw new RuntimeException(e);
      }
    }
  }
}
