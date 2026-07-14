package se.citerus.dddsample.infrastructure.persistence.jpa

import java.util.Optional

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA repository for [[CargoEntity]] — the low-level data
 * access port. Spring generates the proxy at runtime; we get
 * `save / findById / findAll / deleteAll` for free, plus a
 * `findByTrackingId` derived query.
 *
 * This is **not** the [[se.citerus.dddsample.domain.model.cargo.CargoRepository]]
 * the domain expects — it returns persistence entities, not domain
 * aggregates. The adapter [[JpaCargoRepository]] bridges the two.
 */
trait CargoEntityRepository extends JpaRepository[CargoEntity, java.lang.Long]:

  /** Spring Data derives the query from the method name. */
  def findByTrackingId(trackingId: String): Optional[CargoEntity]
