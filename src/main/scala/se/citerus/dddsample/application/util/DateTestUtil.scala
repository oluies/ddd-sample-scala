package se.citerus.dddsample.application.util

import java.util.Date

trait DateTestUtil {

  /**
   * @param date date string as yyyy-MM-dd
   * @return Date representation
   */
  def toDate(date:String) : Date = {
    toDate(date, "00:00.00.000");
  }

  /**
   * @param date date string as yyyy-MM-dd
   * @param time time string as HH:mm
   * @return Date representation
   */
  def toDate(date:String, time:String) : Date = {
    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date + " " + time);
  }
  
}