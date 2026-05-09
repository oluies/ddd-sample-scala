package se.citerus.dddsample.infrastructure.persistence.hibernate

import org.scalatest.funsuite.AnyFunSuite

/**
 * STUB: This test originally extended Spring 2.x's
 * `AbstractTransactionalDataSourceSpringContextTests`, which was removed in
 * Spring 3.0. The Hibernate integration tests need to be ported to Spring 5's
 * `SpringExtension` / `@SpringJUnitConfig` testing model.
 *
 * Until that work happens, this class is a no-op so the test compile passes
 * and the unit tests in other packages can run.
 */
abstract class AbstractRepositoryTest extends AnyFunSuite {
  // TODO(scala3-migration): port from AbstractTransactionalDataSourceSpringContextTests
  // to Spring 5's SpringExtension + @SpringJUnitConfig + @Transactional.
}
