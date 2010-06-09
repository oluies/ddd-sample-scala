package se.citerus.dddsample.domain.model.location

/**
 * Sample locations, for test purposes.
 * 
 */
object SampleLocations {

  val HONGKONG = new Location(new UnLocode("CNHKG"), "Hongkong")
  val MELBOURNE = new Location(new UnLocode("AUMEL"), "Melbourne")
  val STOCKHOLM = new Location(new UnLocode("SESTO"), "Stockholm")
  val HELSINKI = new Location(new UnLocode("FIHEL"), "Helsinki")
  val CHICAGO = new Location(new UnLocode("USCHI"), "Chicago")
  val TOKYO = new Location(new UnLocode("JNTKO"), "Tokyo")
  val HAMBURG = new Location(new UnLocode("DEHAM"), "Hamburg")
  val SHANGHAI = new Location(new UnLocode("CNSHA"), "Shanghai")
  val ROTTERDAM = new Location(new UnLocode("NLRTM"), "Rotterdam")
  val GOTHENBORG = new Location(new UnLocode("SEGOT"), "Gothenborg")
  val HANGZHOU = new Location(new UnLocode("CNHGH"), "Hangzhou")
  val NEWYORK = new Location(new UnLocode("USNYC"), "New York")
  val DALLAS = new Location(new UnLocode("USDAL"), "Dallas")
  
  private val locationList = List(
      HONGKONG,
      MELBOURNE,
      STOCKHOLM,
      HELSINKI,
      CHICAGO,
      TOKYO,
      HAMBURG,
      SHANGHAI,
      ROTTERDAM,
      GOTHENBORG,
      HANGZHOU,
      NEWYORK,
      DALLAS)

  private val locationMap : Map[UnLocode, Location] = Map() ++ locationList.map{ l => (l.unlocode, l) } 

  def getAll() : List[Location] = {
    locationList;
  }

  def lookup(unLocode:UnLocode) : Option[Location] = {
    locationMap.get(unLocode);
  }

}
