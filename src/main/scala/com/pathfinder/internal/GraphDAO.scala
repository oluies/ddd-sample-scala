package com.pathfinder.internal

import util.Random

class GraphDAO {
  val listLocations: List[String] = {
    List("CNHKG", "AUMEL", "SESTO", "FIHEL", "USCHI", "JNTKO", "DEHAM", "CNSHA", "NLRTM", "SEGOT", "CNHGH", "USNYC", "USDAL")
  }

  def getVoyageNumber(from: String, to: String): String = {
    val random = new Random()
    val i = random.nextInt(5);
    i match {
      case 0 => return "0100S";
      case 1 => return "0200T";
      case 2 => return "0300A";
      case 3 => return "0301S";
      case _ => return "0400S";
    }
  }
}