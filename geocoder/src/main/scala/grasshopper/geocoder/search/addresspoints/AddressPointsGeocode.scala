package grasshopper.geocoder.search.addresspoints

import java.net.URLDecoder

import com.typesafe.scalalogging.Logger
import feature._
import geometry.Point
import org.slf4j.LoggerFactory
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import spray.json._
import scala.util.Try
import io.geojson.FeatureJsonProtocol._

trait AddressPointsGeocode {

  lazy val pointLogger = Logger(LoggerFactory.getLogger("grasshopper-grasshopper.addresspoints"))

  def geocodePoint(client: Client, index: String, indexType: String, address: String, count: Int): Array[Feature] = {
    pointLogger.debug(s"Search Address: ${address}")
    val hits = searchAddress(client, index, indexType, address)
    if (hits.length >= 1) {
      hits
        .map(hit => hit.getSourceAsString)
        .take(count)
        .map(s => s.parseJson.convertTo[Feature])
        .map(f => f.addOrUpdate("source", "state-address-points"))
    } else {
      Array(Feature(Point(0, 0)))
    }

  }

  private def searchAddress(client: Client, index: String, indexType: String, address: String): Array[SearchHit] = {
    val qb = QueryBuilders.matchPhraseQuery("address", address)
    val response = client.prepareSearch(index)
      .setTypes(indexType)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      .setQuery(qb)
      .execute
      .actionGet

    response.getHits().getHits
  }
}
