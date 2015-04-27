package grasshopper.elasticsearch

import feature._
import io.geojson.FeatureJsonProtocol._
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import spray.json._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import scala.util.{ Success, Failure, Try }
import org.elasticsearch.search.SearchHit

trait Geocode {

  lazy val log = Logger(LoggerFactory.getLogger("grasshopper-geocode"))

  def geocode(client: Client, index: String, indexType: String, address: String, count: Int): Try[Array[Feature]] = {
    log.debug(s"Search Address: ${address}")
    Try {
      val hits = searchAddress(client, index, indexType, address)
      hits
        .map(hit => hit.getSourceAsString)
        .take(count)
        .map { s =>
          log.info(s)
          s.parseJson.convertTo[Feature]
        }
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
