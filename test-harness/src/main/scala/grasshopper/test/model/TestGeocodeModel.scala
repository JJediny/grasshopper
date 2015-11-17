package grasshopper.test.model

import geometry.Point
import grasshopper.test.util.Haversine

object TestGeocodeModel {
  object PointInputAddress {
    def empty: PointInputAddress = PointInputAddress("", Point(0, 0))
  }

  case class PointInputAddress(inputAddress: String, point: Point)

  object PointInputAddressTract {
    def empty: PointInputAddressTract = PointInputAddressTract(PointInputAddress.empty, "")
  }

  case class PointInputAddressTract(pointInputAddress: PointInputAddress, geoid: String) {
    def toCSV: String = s"${pointInputAddress.inputAddress},${pointInputAddress.point.x},${pointInputAddress.point.y},${geoid}"
  }

  case class CensusOverlayResult(inputPoint: PointInputAddressTract, outputPoint: PointInputAddressTract) {

    def dist: Double = Haversine.distance(this.inputPoint.pointInputAddress.point, this.outputPoint.pointInputAddress.point)

    def toCSV: String = s"${inputPoint.pointInputAddress.inputAddress}," +
      s"${inputPoint.pointInputAddress.point.x}," +
      s"${inputPoint.pointInputAddress.point.y}," +
      s"${inputPoint.geoid}," +
      s"${outputPoint.pointInputAddress.point.x}," +
      s"${outputPoint.pointInputAddress.point.y}," +
      s"${dist}," +
      s"${outputPoint.geoid}\n"

  }
}
