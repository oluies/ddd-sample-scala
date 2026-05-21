package se.citerus.dddsample.infrastructure.persistence.magnum

import java.time.{Instant, ZoneOffset}

import com.augustnagro.magnum.DbCodec

/**
 * Custom `DbCodec`s for types Magnum 1.3 doesn't ship by default.
 *
 * `Instant` is the type used throughout the domain for points in time
 * (`HandlingEvent.completionTime`, `RouteSpecification.arrivalDeadline`,
 * etc.). Magnum's built-in `OffsetDateTimeCodec` handles JDBC's
 * `TIMESTAMP WITH TIME ZONE` natively, so we adapt that via `biMap`.
 * All `Instant`s round-trip as UTC.
 */
object MagnumCodecs:

  given DbCodec[Instant] = DbCodec.OffsetDateTimeCodec.biMap(
    odt => odt.toInstant,
    inst => inst.atOffset(ZoneOffset.UTC)
  )
