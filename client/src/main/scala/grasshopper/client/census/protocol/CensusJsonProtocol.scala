package grasshopper.client.census.protocol

import grasshopper.model.census.CensusResult
import grasshopper.protocol.StatusJsonProtocol
import spray.json.DefaultJsonProtocol
import io.geojson.FeatureJsonProtocol._

trait CensusJsonProtocol extends DefaultJsonProtocol with StatusJsonProtocol {
  implicit val censusResultFormat = jsonFormat2(CensusResult.apply)
}
