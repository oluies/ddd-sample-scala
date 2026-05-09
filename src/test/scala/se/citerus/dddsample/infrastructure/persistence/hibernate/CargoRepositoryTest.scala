package se.citerus.dddsample.infrastructure.persistence.hibernate

/**
 * STUB: Original tests exercised `CargoRepositoryHibernate` against an
 * HSQLDB-backed Spring context using Spring 2.x test framework
 * (`AbstractTransactionalDataSourceSpringContextTests`). The integration
 * tests need a port to Spring 5 testing.
 *
 * The original assertions are preserved as TODO comments so the rewrite
 * has a starting point.
 *
 * TODO(scala3-migration): port to Spring 5 TestContext.
 *  - testFindByCargoId: find tracking id "FGH", assert origin/route, walk
 *    handling history, assert legs.
 *  - testFindByCargoIdUnknownId: cargoRepository.find(unknown) === None.
 *  - testSave: store a new Cargo, flush, verify persisted columns.
 *  - testReplaceItinerary: re-route an existing Cargo, verify Leg row count.
 *  - testFindAll: assert sample fixture has 6 cargos.
 *  - testNextTrackingId: assert two consecutive ids differ.
 */
class CargoRepositoryTest extends AbstractRepositoryTest {
  // No tests until the AbstractRepositoryTest port is done.
}
