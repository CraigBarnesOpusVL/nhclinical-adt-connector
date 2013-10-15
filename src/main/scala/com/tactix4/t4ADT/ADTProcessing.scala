package com.tactix4.t4ADT

import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.HL7Exception
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import scala.util.control.Exception.catching
/**
 * @author max@tactix4.com
 *         11/10/2013
 */
trait ADTProcessing {

  /**
   * query a terser object for a specified path
   * @param s the terser path
   * @param t the terser object to query
   * @return the value from the specified terser path
   * @throws ADTParseException if there was an error parsing the supplied path
   * @throws ADTFieldException if the field was empty
   */
  def getValueFromTerserPath(s: String, t: Terser): String  = {
    try {
      t.get(s) match {
        case null => throw new ADTFieldException("no value found at terser path: " + s)
        case someString:String => someString
      }
    }
    catch {
      case e: HL7Exception => throw new ADTFieldException(e.getMessage)
      case e: IllegalArgumentException => throw new ADTApplicationException("Error in terser path: " + e.getMessage)
    }
  }

  /**
   * Convenience method to check a valid date
   *
   * @param date the date string
   * @return a [[scalaz.ValidationNel[String,T] ]]
   */
  def checkDate(date: String,dateTimeFormat:DateTimeFormatter): String = {
    try { (DateTime.parse(date, dateTimeFormat)).toString() }
    catch { case e: Exception => throw new ADTFieldException("unable to parse date: " + date) }
  }

  def getMessageTypeMap(terserMap:Map[String, Map[String,String]], messageType:String): Map[String, String] = {
    terserMap.get(messageType).getOrElse(throw new ADTApplicationException("Could not find terser configuration for messages of type: "+ messageType))
  }
  def getTerserPathFromMap(messageTypeMap: Map[String,String], attribute: String): String = {
    messageTypeMap.get(attribute).getOrElse(throw new ADTApplicationException("Could not find attribute: " + attribute + " in terser configuration"))
  }

  def getAttribute(messageTypeMap: Map[String, String], attribute: String, terser: Terser): (String, String) = {
    val path = getTerserPathFromMap(messageTypeMap, attribute)
    val value = getValueFromTerserPath(path, terser)
    attribute -> value
  }

  def getMessageType(terser: Terser):String = {
    getValueFromTerserPath("MSH-9-2", terser)
  }

  def validateRequiredFields(fields: List[String], mappings: Map[String,String], terser: Terser ) : Map[String,String]= {
    fields.map(f => getAttribute(mappings,f,terser)).toMap
  }

  def validateOptionalFields(fields: List[String], mappings: Map[String,String], terser: Terser): Map[String, String] = {
    fields.map(f => catching(classOf[ADTFieldException], classOf[ADTApplicationException]).opt(getAttribute(mappings,f,terser))).flatten.toMap
  }

  def getOptionalFields(mappings:Map[String,String], requiredFields: Map[String,String]): List[String] = {
    (mappings.keySet diff requiredFields.keySet).toList
  }

  def getMappings(terser:Terser, terserMap:Map[String, Map[String,String]]) = {
    getMessageTypeMap(terserMap, getMessageType(terser))
  }


  def getIdentifiers(mappings:Map[String,String],terser:Terser): Map[String, String] = {
    val identifiers = validateOptionalFields(List("patientId", "otherId"),mappings, terser)
    if(identifiers.isEmpty) throw new ADTFieldException("Identifier not found in message")
    else identifiers.toMap
  }
}
