package se.citerus.dddsample.infrastructure.sampledata

import java.time.Instant

import se.citerus.dddsample.application.util.DateUtils.toDate
import se.citerus.dddsample.domain.model.voyage.{CarrierMovement, Schedule, Voyage, VoyageNumber}
import se.citerus.dddsample.infrastructure.sampledata.SampleLocations.*

/** Sample voyages used by tests and the bootstrap data loader.
  *
  * Two families:
  *   - `CM001`..`CM006` — single-movement voyages (deprecated upstream but
  *     still referenced by tests).
  *   - Multi-movement named voyages (`HONGKONG_TO_NEW_YORK`, etc.) — the
  *     ones the sample-data generator wires into cargo itineraries.
  */
object SampleVoyages:

  private def singleHop(id: String, from: se.citerus.dddsample.domain.model.location.Location, to: se.citerus.dddsample.domain.model.location.Location): Voyage =
    new Voyage(
      VoyageNumber(id),
      Schedule(List(CarrierMovement(from, to, Instant.now(), Instant.now())))
    )

  val CM001: Voyage = singleHop("CM001", STOCKHOLM, HAMBURG)
  val CM002: Voyage = singleHop("CM002", HAMBURG,   HONGKONG)
  val CM003: Voyage = singleHop("CM003", HONGKONG,  NEWYORK)
  val CM004: Voyage = singleHop("CM004", NEWYORK,   CHICAGO)
  val CM005: Voyage = singleHop("CM005", CHICAGO,   HAMBURG)
  val CM006: Voyage = singleHop("CM006", HAMBURG,   HANGZHOU)

  val HONGKONG_TO_NEW_YORK: Voyage =
    new Voyage.Builder(VoyageNumber("0100S"), HONGKONG)
      .addMovement(HANGZHOU,  toDate("2008-10-01", "12:00"), toDate("2008-10-03", "14:30"))
      .addMovement(TOKYO,     toDate("2008-10-03", "21:00"), toDate("2008-10-06", "06:15"))
      .addMovement(MELBOURNE, toDate("2008-10-06", "11:00"), toDate("2008-10-12", "11:30"))
      .addMovement(NEWYORK,   toDate("2008-10-14", "12:00"), toDate("2008-10-23", "23:10"))
      .build()

  val NEW_YORK_TO_DALLAS: Voyage =
    new Voyage.Builder(VoyageNumber("0200T"), NEWYORK)
      .addMovement(CHICAGO, toDate("2008-10-24", "07:00"), toDate("2008-10-24", "17:45"))
      .addMovement(DALLAS,  toDate("2008-10-24", "21:25"), toDate("2008-10-25", "19:30"))
      .build()

  val DALLAS_TO_HELSINKI: Voyage =
    new Voyage.Builder(VoyageNumber("0300A"), DALLAS)
      .addMovement(HAMBURG,   toDate("2008-10-29", "03:30"), toDate("2008-10-31", "14:00"))
      .addMovement(STOCKHOLM, toDate("2008-11-01", "15:20"), toDate("2008-11-01", "18:40"))
      .addMovement(HELSINKI,  toDate("2008-11-02", "09:00"), toDate("2008-11-02", "11:15"))
      .build()

  val DALLAS_TO_HELSINKI_ALT: Voyage =
    new Voyage.Builder(VoyageNumber("0301S"), DALLAS)
      .addMovement(HELSINKI, toDate("2008-10-29", "03:30"), toDate("2008-11-05", "15:45"))
      .build()

  val HELSINKI_TO_HONGKONG: Voyage =
    new Voyage.Builder(VoyageNumber("0400S"), HELSINKI)
      .addMovement(ROTTERDAM, toDate("2008-11-04", "05:50"), toDate("2008-11-06", "14:10"))
      .addMovement(SHANGHAI,  toDate("2008-11-10", "21:45"), toDate("2008-11-22", "16:40"))
      .addMovement(HONGKONG,  toDate("2008-11-24", "07:00"), toDate("2008-11-28", "13:37"))
      .build()

  val all: List[Voyage] = List(
    CM001, CM002, CM003, CM004, CM005, CM006,
    HONGKONG_TO_NEW_YORK, NEW_YORK_TO_DALLAS,
    DALLAS_TO_HELSINKI, DALLAS_TO_HELSINKI_ALT, HELSINKI_TO_HONGKONG
  )

  private val byNumber: Map[VoyageNumber, Voyage] = all.map(v => v.voyageNumber -> v).toMap

  def lookup(voyageNumber: VoyageNumber): Option[Voyage] = byNumber.get(voyageNumber)
