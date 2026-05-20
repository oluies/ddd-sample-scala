package se.citerus.dddsample.infrastructure.sampledata

import se.citerus.dddsample.domain.model.location.{Location, UnLocode}

/** Sample [[Location]]s used by tests and the bootstrap data loader. */
object SampleLocations:

  val HONGKONG:   Location = Location(UnLocode("CNHKG"), "Hongkong")
  val MELBOURNE:  Location = Location(UnLocode("AUMEL"), "Melbourne")
  val STOCKHOLM:  Location = Location(UnLocode("SESTO"), "Stockholm")
  val HELSINKI:   Location = Location(UnLocode("FIHEL"), "Helsinki")
  val CHICAGO:    Location = Location(UnLocode("USCHI"), "Chicago")
  val TOKYO:      Location = Location(UnLocode("JNTKO"), "Tokyo")
  val HAMBURG:    Location = Location(UnLocode("DEHAM"), "Hamburg")
  val SHANGHAI:   Location = Location(UnLocode("CNSHA"), "Shanghai")
  val ROTTERDAM:  Location = Location(UnLocode("NLRTM"), "Rotterdam")
  val GOTHENBURG: Location = Location(UnLocode("SEGOT"), "Göteborg")
  val HANGZHOU:   Location = Location(UnLocode("CNHGH"), "Hangzhou")
  val NEWYORK:    Location = Location(UnLocode("USNYC"), "New York")
  val DALLAS:     Location = Location(UnLocode("USDAL"), "Dallas")

  val all: List[Location] = List(
    HONGKONG, MELBOURNE, STOCKHOLM, HELSINKI, CHICAGO, TOKYO, HAMBURG,
    SHANGHAI, ROTTERDAM, GOTHENBURG, HANGZHOU, NEWYORK, DALLAS
  )

  private val byUnLocode: Map[UnLocode, Location] = all.map(l => l.unLocode -> l).toMap

  def lookup(unLocode: UnLocode): Option[Location] = byUnLocode.get(unLocode)
