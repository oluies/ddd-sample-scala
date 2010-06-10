package se.citerus.dddsample.infrastructure.persistence.hibernate

import org.springframework.stereotype.Repository;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;

@Repository
class CargoRepositoryHibernate extends HibernateRepository with CargoRepository {

  def find(tid:TrackingId) : Option[Cargo] = {
    val result = getSession().
      createQuery("from Cargo where trackingId = :tid").
      setParameter("tid", tid).
      uniqueResult();
    val cargo:Cargo = result.asInstanceOf[Cargo] 
    Some(cargo)
  }

  def store(cargo:Cargo) : Unit = {
    getSession.saveOrUpdate(cargo);
    // Delete-orphan does not seem to work correctly when the parent is a component
    getSession.createSQLQuery("delete from Leg where cargo_id = null").executeUpdate();
  }

  def nextTrackingId() : TrackingId = {
    // TODO use an actual DB sequence here, UUID is for in-mem
    val random = java.util.UUID.randomUUID().toString().toUpperCase();
    return new TrackingId(
      random.substring(0, random.indexOf("-"))
    );
  }

  def findAll() : List[Cargo] = {
    val query = getSession().createQuery("from Cargo")
    val cargoList = List[Cargo]() ++ query.list().asInstanceOf[List[Cargo]];
    cargoList
  }
}