package addresspoints.api

import java.util.Calendar
import addresspoints.model.{ AddressInput, Status }
import addresspoints.protocol.JsonProtocol
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.marshalling.ToResponseMarshallable
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.model.StatusCodes._
import akka.http.server.Directives._
import akka.http.server.StandardRoute
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.Config
import grasshopper.elasticsearch.Geocode
import org.elasticsearch.client.Client
import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Try, Failure, Success }
import spray.json._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import io.geojson.FeatureJsonProtocol._

trait Service extends JsonProtocol with Geocode {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: ActorFlowMaterializer
  implicit val client: Client

  def config: Config

  val logger: LoggingAdapter

  override lazy val log = Logger(LoggerFactory.getLogger("grasshopper-address-points"))

  val routes = {
    path("status") {
      get {
        compressResponseIfRequested() {
          complete {
            val now = Calendar.getInstance().getTime()
            val status = Status("OK", now.toString)
            log.info(status.toJson.toString())
            ToResponseMarshallable(status)
          }
        }
      }
    } ~
      pathPrefix("addresses") {
        path("points") {
          post {
            compressResponseIfRequested() {
              entity(as[String]) { json =>
                try {
                  val addressInput = json.parseJson.convertTo[AddressInput]
                  geocodePoint(addressInput.address)
                } catch {
                  case e: spray.json.DeserializationException =>
                    complete(BadRequest)
                }
              }
            }
          }
        } ~
          path("points" / Segment) { address =>
            get {
              compressResponseIfRequested() {
                geocodePoint(address)
              }
            }
          }
      }
  }

  private def geocodePoint(address: String): StandardRoute = {
    val point = geocode(client, "address", "point", address)
    point match {
      case Success(p) =>
        complete {
          ToResponseMarshallable(p)
        }
      case Failure(_) =>
        complete {
          NotFound
        }
    }
  }
}
