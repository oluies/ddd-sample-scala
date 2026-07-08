package se.citerus.dddsample.infrastructure.persistence.magnum

import java.time.Instant

import com.augustnagro.magnum.*

import se.citerus.dddsample.infrastructure.persistence.magnum.MagnumCodecs.given

/**
 * Persistence model for one entry in a Cargo's `Itinerary`.
 *
 * Stored in a separate `leg` table joined back to `cargo` by FK. The
 * `legIndex` column preserves leg order on rehydration without relying on
 * insert order. References to other aggregates are by id:
 *
 *   - `voyageNumber`     → resolved via `VoyageRepository`
 *   - `loadUnLocode`     → resolved via `LocationRepository`
 *   - `unloadUnLocode`   → resolved via `LocationRepository`
 */
@Table(H2DbType, SqlNameMapper.CamelToSnakeCase)
final case class LegRow(
    @Id id: Long,
    cargoId: Long,
    legIndex: Int,
    voyageNumber: String,
    loadUnLocode: String,
    unloadUnLocode: String,
    loadTime: Instant,
    unloadTime: Instant
) derives DbCodec

final case class LegCreator(
    cargoId: Long,
    legIndex: Int,
    voyageNumber: String,
    loadUnLocode: String,
    unloadUnLocode: String,
    loadTime: Instant,
    unloadTime: Instant
) derives DbCodec
