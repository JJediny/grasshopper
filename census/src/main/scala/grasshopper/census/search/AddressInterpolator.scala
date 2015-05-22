package grasshopper.census.search

import geometry._
import feature._
import io.geojson.FeatureJsonProtocol._
import grasshopper.census.model.AddressRange

object AddressInterpolator {

  def calculateAddressRange(f: Feature, a: Int): AddressRange = {
    val addressIsEven = a % 2 == 0
    val rEven = f.values.getOrElse("RFROMHN", 0)
    val rightRange = rEven match {
      case "" => 0
      case _ => rEven.toString.toInt
    }
    val rightRangeIsEven = rightRange % 2 == 0

    val lEven = f.values.getOrElse("LFROMHN", 0)
    val leftRange = lEven match {
      case "" => 0
      case _ => lEven.toString.toInt
    }
    val leftRangeIsEven = leftRange % 2 == 0

    val prefix =
      if (addressIsEven && rightRangeIsEven)
        "R"
      else if (addressIsEven && leftRangeIsEven)
        "L"
      else if (!addressIsEven && !rightRangeIsEven)
        "R"
      else if (!addressIsEven && !leftRangeIsEven)
        "L"

    val start = f.values.getOrElse(s"${prefix}FROMHN", "0").toString.toInt
    val end = f.values.getOrElse(s"${prefix}TOHN", "0").toString.toInt

    AddressRange(start, end)
  }

  def interpolate(feature: Feature, range: AddressRange, a: Int): Feature = {
    val sign = if (a % 2 == 0) -1 else 1
    val line = feature.geometry.asInstanceOf[Line]
    val l = line.length
    val d = calculateDistance(range)
    val x = a - range.start
    val dist = x * l / d
    //TODO: Review how offset is being calculated
    val geometry = line.pointAtDistWithOffset((dist * -1), sign * 0.0001)
    val addressField = Field("address", StringType())
    val geomField = Field("geometry", GeometryType())
    val numberField = Field("number", IntType())
    val schema = Schema(geomField, addressField, numberField)
    val fullname = feature.values.getOrElse("FULLNAME", "")
    val zipL = feature.values.getOrElse("ZIPL", "")
    val zipR = feature.values.getOrElse("ZIPR", "")
    val lfromhn = feature.values.getOrElse("LFROMHN", "")
    val ltohn = feature.values.getOrElse("LTOHN", "")
    val rfromhn = feature.values.getOrElse("RFROMHN", "")
    val rtohn = feature.values.getOrElse("RTOHN", "")
    val state = feature.values.getOrElse("STATE", "")
    val values: Map[String, Any] = Map(
      "geometry" -> geometry,
      "FULLNAME" -> fullname,
      "RFROMHN" -> rfromhn,
      "RTOHN" -> rtohn,
      "LFROMHN" -> lfromhn,
      "LTOHN" -> ltohn,
      "ZIPR" -> zipR,
      "ZIPL" -> zipL,
      "STATE" -> state
    )
    Feature(schema, values)
  }

  private def calculateDistance(range: AddressRange): Double = {
    val start = range.start
    val end = range.end
    if (start < end)
      start - end
    else
      end - start
  }
}
