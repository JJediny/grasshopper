package grasshopper.geocoder.api

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.coding.{ Deflate, Gzip, NoCoding }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import feature.Feature
import grasshopper.client.protocol.ClientJsonProtocol
import io.geojson.FeatureJsonProtocol._
import grasshopper.client.addresspoints.AddressPointsClient
import grasshopper.client.addresspoints.model.{ AddressPointsResult, AddressPointsStatus }
import grasshopper.client.census.CensusClient
import grasshopper.client.census.model.{ CensusResult, CensusStatus, ParsedInputAddress }
import grasshopper.client.model.ResponseError
import grasshopper.client.parser.AddressParserClient
import grasshopper.client.parser.model.{ ParsedAddress, ParserStatus }
import grasshopper.geocoder.model.{ GeocodeResult, GeocodeStatus }
import grasshopper.geocoder.protocol.GrasshopperJsonProtocol
import io.geojson.FeatureJsonProtocol._
import org.slf4j.LoggerFactory
import scala.async.Async.{ async, await }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import spray.json._

trait Service extends GrasshopperJsonProtocol with ClientJsonProtocol {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor
  implicit val materializer: ActorFlowMaterializer

  def config: Config

  val logger: LoggingAdapter

  lazy val log = Logger(LoggerFactory.getLogger("grashopper-geocoder"))

  val routes = {
    path("status") {
      val fStatus: Future[GeocodeStatus] = async {
        val as = AddressPointsClient.status.map(s => s.right.getOrElse(AddressPointsStatus.empty))
        val cs = CensusClient.status.map(s => s.right.getOrElse(CensusStatus.empty))
        val ps = AddressParserClient.status.map(s => s.right.getOrElse(ParserStatus.empty))
        GeocodeStatus(await(as), await(cs), await(ps))
      }

      encodeResponseWith(NoCoding, Gzip, Deflate) {
        complete {
          fStatus.map { s =>
            ToResponseMarshallable(s)
          }
        }
      }
    } ~
      path("geocode" / Segment) { address =>

        val fParsed: Future[(ParsedAddress, ParsedInputAddress)] = async {
          val addr = await(AddressParserClient.parse(address))
          if (addr.isLeft) {
            log.error(addr.left.get.desc)
            (ParsedAddress.empty, ParsedInputAddress.empty)
          } else {
            val parsedAddress = addr.right.getOrElse(ParsedAddress.empty)
            val parsedInputAddress = ParsedInputAddress(
              parsedAddress.parts.AddressNumber.toInt,
              parsedAddress.parts.StreetName,
              parsedAddress.parts.ZipCode.toInt,
              parsedAddress.parts.StateName
            )
            (parsedAddress, parsedInputAddress)
          }
        }

        val fGeocoded = async {
          val parsed = await(fParsed)
          val parsedAddress = parsed._1
          val parsedInputAddress = parsed._2
          println(parsedAddress)

          val ptGeocode = await(AddressPointsClient.geocode(address))
          val addressPointGeocode: AddressPointsResult =
            if (ptGeocode.isLeft) {
              log.error(ptGeocode.left.get.desc)
              AddressPointsResult.empty
            } else {
              ptGeocode.right.getOrElse(AddressPointsResult.empty)
            }

          val censusPointGeocode: CensusResult =
            if (parsedInputAddress.isEmpty) {
              CensusResult.empty
            } else {
              val cGeocode = await(CensusClient.geocode(parsedInputAddress))
              if (cGeocode.isLeft) {
                log.error(cGeocode.left.get.desc)
                CensusResult.empty
              } else {
                cGeocode.right.getOrElse(CensusResult.empty)
              }
            }
          GeocodeResult("OK", parsedAddress, addressPointGeocode, censusPointGeocode)
        }

        encodeResponseWith(NoCoding, Gzip, Deflate) {
          complete {
            fGeocoded.map { g =>
              ToResponseMarshallable(g)
            }
          }
        }

      }

  }

}
