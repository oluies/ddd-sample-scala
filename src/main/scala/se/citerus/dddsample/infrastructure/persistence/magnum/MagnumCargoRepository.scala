package se.citerus.dddsample.infrastructure.persistence.magnum

import java.util.UUID
import javax.sql.DataSource

import com.augustnagro.magnum.*

import se.citerus.dddsample.domain.model.cargo.*
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository
import se.citerus.dddsample.domain.model.location.{LocationRepository, UnLocode}
import se.citerus.dddsample.domain.model.voyage.{VoyageNumber, VoyageRepository}
import se.citerus.dddsample.infrastructure.persistence.magnum.MagnumCodecs.given

/**
 * Magnum-backed [[CargoRepository]] — proof-of-concept for phase 9b.
 *
 * Lives alongside `InMemoryCargoRepository`; not yet wired into Spring
 * beans. Activation will be a `@Profile("magnum")` switch in a follow-up
 * if we go ahead with the full 4-aggregate Magnum port.
 *
 * Design choices visible in this single file:
 *
 *   1. **Persistence model is immutable.** `CargoRow` / `LegRow` are case
 *      classes with `derives DbCodec`; no `var`, no no-arg constructor,
 *      no `@Entity` / `@OneToMany` annotations.
 *
 *   2. **Cross-aggregate references stored by id.** `voyageNumber`,
 *      `loadUnLocode`, etc. are plain strings in storage; the adapter
 *      resolves them to live `Voyage` / `Location` via their repositories
 *      during rehydration. Cleaner aggregate boundary than the
 *      object-reference shape inside the domain.
 *
 *   3. **`Delivery` is not persisted.** It is recomputed from the cargo's
 *      handling history on every read (`Cargo.deriveDeliveryProgress`),
 *      avoiding a snapshot that could drift from the source-of-truth
 *      `HandlingEvent` aggregate.
 *
 *   4. **Transactions live in the adapter.** Each method opens its own
 *      `transact(ds) { ... }` block. Application services drop
 *      `@Transactional` once we adopt this layer wholesale — the storage
 *      concern owns transactionality, which is arguably better DDD.
 */
final class MagnumCargoRepository(
    ds: DataSource,
    locationRepository: LocationRepository,
    voyageRepository: VoyageRepository,
    handlingEventRepository: HandlingEventRepository
) extends CargoRepository:

  // Magnum's Repo[EC, E, ID] typeclass: EC is the insert-time row, E the
  // full row (with id), ID the primary-key type.
  private val cargoRepo = Repo[CargoCreator, CargoRow, Long]
  private val legRepo   = Repo[LegCreator, LegRow, Long]

  override def find(trackingId: TrackingId): Option[Cargo] =
    connect(ds):
      sql"select * from cargo_row where tracking_id = ${trackingId.idString}"
        .query[CargoRow]
        .run()
        .headOption
        .map(rehydrate)

  override def getAll: List[Cargo] =
    connect(ds):
      cargoRepo.findAll.toList.map(rehydrate)

  override def store(cargo: Cargo): Unit =
    transact(ds):
      val existingId =
        sql"select id from cargo_row where tracking_id = ${cargo.trackingId.idString}"
          .query[Long]
          .run()
          .headOption

      val cargoId = existingId match
        case Some(id) =>
          sql"""update cargo_row set
                origin_un_locode = ${cargo.origin.unLocode.idString},
                spec_destination_un_locode = ${cargo.routeSpecification.destination.unLocode.idString},
                spec_arrival_deadline = ${cargo.routeSpecification.arrivalDeadline}
                where id = $id""".update.run()
          id
        case None =>
          // insertReturning gives us the generated key in one round-trip.
          val inserted = cargoRepo.insertReturning(toCargoCreator(cargo))
          inserted.id

      // Itinerary is owned by the Cargo aggregate — refresh-by-replace is
      // safe and avoids tracking individual leg-row deltas. Cheap because
      // itineraries are short (handful of legs).
      sql"delete from leg_row where cargo_id = $cargoId".update.run()
      cargo.itineraryOpt.foreach { itin =>
        itin.legs.zipWithIndex.foreach { case (leg, idx) =>
          legRepo.insert(toLegCreator(cargoId, idx, leg))
        }
      }

  override def nextTrackingId(): TrackingId =
    TrackingId(UUID.randomUUID().toString.toUpperCase.substring(0, 10))

  // ----- Row → Aggregate -------------------------------------------------

  private def rehydrate(row: CargoRow)(using DbCon): Cargo =
    val origin =
      locationRepository
        .find(UnLocode(row.originUnLocode))
        .getOrElse(
          throw new IllegalStateException(s"Unknown origin UN/Locode: ${row.originUnLocode}")
        )
    val destination =
      locationRepository
        .find(UnLocode(row.specDestinationUnLocode))
        .getOrElse(
          throw new IllegalStateException(
            s"Unknown destination UN/Locode: ${row.specDestinationUnLocode}"
          )
        )
    val spec = RouteSpecification(origin, destination, row.specArrivalDeadline)

    val withSpec = Cargo(TrackingId(row.trackingId), spec)
    val withItinerary = readLegs(row.id) match
      case Some(itin) => withSpec.assignToRoute(itin)
      case None       => withSpec

    val history = handlingEventRepository.lookupHandlingHistoryOfCargo(withItinerary.trackingId)
    withItinerary.deriveDeliveryProgress(history)

  private def readLegs(cargoId: Long)(using DbCon): Option[Itinerary] =
    val rows = sql"select * from leg_row where cargo_id = $cargoId order by leg_index"
      .query[LegRow]
      .run()
    if rows.isEmpty then None
    else
      Some(Itinerary(rows.map { r =>
        Leg(
          voyageRepository
            .find(VoyageNumber(r.voyageNumber))
            .getOrElse(throw new IllegalStateException(s"Unknown voyage: ${r.voyageNumber}")),
          locationRepository
            .find(UnLocode(r.loadUnLocode))
            .getOrElse(throw new IllegalStateException(s"Unknown load loc: ${r.loadUnLocode}")),
          locationRepository
            .find(UnLocode(r.unloadUnLocode))
            .getOrElse(throw new IllegalStateException(s"Unknown unload loc: ${r.unloadUnLocode}")),
          r.loadTime,
          r.unloadTime
        )
      }.toList))

  // ----- Aggregate → Row -------------------------------------------------

  private def toCargoCreator(cargo: Cargo): CargoCreator =
    CargoCreator(
      trackingId = cargo.trackingId.idString,
      originUnLocode = cargo.origin.unLocode.idString,
      specDestinationUnLocode = cargo.routeSpecification.destination.unLocode.idString,
      specArrivalDeadline = cargo.routeSpecification.arrivalDeadline
    )

  private def toLegCreator(cargoId: Long, legIndex: Int, leg: Leg): LegCreator =
    LegCreator(
      cargoId = cargoId,
      legIndex = legIndex,
      voyageNumber = leg.voyage.voyageNumber.idString,
      loadUnLocode = leg.loadLocation.unLocode.idString,
      unloadUnLocode = leg.unloadLocation.unLocode.idString,
      loadTime = leg.loadTime,
      unloadTime = leg.unloadTime
    )
