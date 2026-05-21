package se.citerus.dddsample.infrastructure.persistence.magnum

import java.time.Instant

import com.augustnagro.magnum.*

import se.citerus.dddsample.infrastructure.persistence.magnum.MagnumCodecs.given

/**
 * Persistence model for the Cargo aggregate root.
 *
 * Pure data — no domain methods, no JPA annotations, no `var` fields.
 * Magnum derives a `DbCodec` at compile time that maps these fields to
 * columns in the `cargo` table. Mapping between this row and the domain
 * [[se.citerus.dddsample.domain.model.cargo.Cargo]] aggregate lives in
 * [[MagnumCargoRepository]].
 *
 * Cross-aggregate references stored by **id** (UN/Locode, voyage number)
 * — matches the DDD recommendation that aggregates reference each other
 * by id, not by object. The domain's `Cargo` then resolves those ids via
 * the `Location` / `Voyage` repositories during rehydration.
 *
 * The `Delivery` snapshot is **not** persisted. It is re-derived from
 * `RouteSpecification` + `Itinerary` + the cargo's handling history every
 * time a `Cargo` is loaded — mirroring `Cargo.deriveDeliveryProgress` —
 * which keeps storage normalized and avoids stale snapshots.
 */
@Table(H2DbType, SqlNameMapper.CamelToSnakeCase)
final case class CargoRow(
    @Id id: Long,
    trackingId: String,
    originUnLocode: String,
    specDestinationUnLocode: String,
    specArrivalDeadline: Instant
) derives DbCodec

/**
 * The fields needed to *insert* a new Cargo row. Magnum's `Repo` typeclass
 * takes a separate creator type so id-on-write is the database's job.
 */
final case class CargoCreator(
    trackingId: String,
    originUnLocode: String,
    specDestinationUnLocode: String,
    specArrivalDeadline: Instant
) derives DbCodec
