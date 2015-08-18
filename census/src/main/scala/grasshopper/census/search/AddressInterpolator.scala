package grasshopper.census.search

import feature._
import geometry._
import grasshopper.census.model.AddressRange

object AddressInterpolator {

  def calculateAddressRange(f: Feature, a: Int): AddressRange = {
    val pre: String = prefix(f, a)

    val s = f.values.getOrElse(s"${pre}FROMHN", "0")
    val e = f.values.getOrElse(s"${pre}TOHN", "0")

    val start = s match {
      case "" => 0
      case _ => s.toString.toInt
    }
    val end = e match {
      case "" => 0
      case _ => e.toString.toInt
    }

    AddressRange(start, end)
  }

  def interpolate(feature: Feature, range: AddressRange, a: Int): Feature = {
    val pre = prefix(feature, a)
    val sign = if (pre == "R") -1 else 1
    val line = feature.geometry.asInstanceOf[Line]
    val l = line.length
    val d = calculateDistance(range)
    val x = a - range.start
    val dist = x * l / d
    val geometry = line.pointAtDistWithOffset(dist, sign * 0.0001)
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

  private def prefix(f: Feature, a: Int): String = {
    val addressIsEven = a % 2 == 0
    val rightRangeIsEven: Boolean = rangeIsEven(f, false)

    val leftRangeIsEven: Boolean = rangeIsEven(f, true)

    if (addressIsEven && rightRangeIsEven)
      "R"
    else if (addressIsEven && leftRangeIsEven)
      "L"
    else if (!addressIsEven && !rightRangeIsEven)
      "R"
    else if (!addressIsEven && !leftRangeIsEven)
      "L"
    else
      "R"
  }

  private def rangeIsEven(f: Feature, isLeft: Boolean): Boolean = {
    val prefix = if (isLeft) "L" else "R"
    val fromEven = f.values.getOrElse(s"${prefix}FROMHN", 0)
    val fromRange = fromEven match {
      case "" => 0
      case _ => fromEven.toString.toInt
    }
    val fromIsEven = fromEven != "" && fromRange % 2 == 0

    val toEven = f.values.getOrElse(s"${prefix}TOHN", 0)
    val toRange = fromEven match {
      case "" => 0
      case _ => toEven.toString.toInt
    }
    val toIsEven = toEven != "" && toRange % 2 == 0
    fromIsEven || toIsEven
  }

  private def calculateDistance(range: AddressRange): Double = {
    val start = range.start
    val end = range.end
    if (start < end)
      end - start
    else
      start - end
  }
}
