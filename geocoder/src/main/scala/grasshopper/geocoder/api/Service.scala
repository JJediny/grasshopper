package grasshopper.geocoder.api

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.coding.{ Deflate, Gzip, NoCoding }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import grasshopper.client.addresspoints.AddressPointsClient
import grasshopper.client.addresspoints.model.{ AddressPointsResult, AddressPointsStatus }
import grasshopper.client.census.CensusClient
import grasshopper.client.census.model.{ CensusResult, CensusStatus, ParsedInputAddress }
import grasshopper.client.parser.AddressParserClient
import grasshopper.client.parser.model.{ ParsedAddress, ParserStatus }
import grasshopper.client.protocol.ClientJsonProtocol
import grasshopper.geocoder.model.{ GeocodeResult, GeocodeStatus }
import grasshopper.geocoder.protocol.GrasshopperJsonProtocol
import org.slf4j.LoggerFactory

import scala.async.Async.{ async, await }
import scala.concurrent.{ ExecutionContextExecutor, Future }

trait Service extends GrasshopperJsonProtocol with ClientJsonProtocol {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor
  implicit val materializer: ActorMaterializer

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
      path("geocode") {
        post {
          //          extractRequest { request =>
          //            request.entity.dataBytes
          //              .via(
          //                Framing.delimiter(ByteString("\r\n"), maximumFrameLength = 1000, allowTruncation = true)
          //              ).map(_.utf8String)
          //              .to(Sink.foreach(println)).run()
          //            complete("uploaded")
          //          }
          entity(as[FormData]) { formData =>
            val list = formData.parts.map { bodyPart =>
              bodyPart.entity.dataBytes.map { byteString =>
                Source(List(byteString)).map { e =>
                  e.utf8String
                  //Files.write(tempFile, e.toArray, StandardOpenOption.APPEND)
                }
              }
            }
            complete { "OK" }
          }
          //          entity(as[FormData]) { formData =>
          //            val parts = formData.parts
          //            parts.runForeach { bodyPart =>
          //              bodyPart.entity.dataBytes.runForeach { byteString =>
          //                Source(List(byteString)).runForeach { e =>
          //                  println(e.toArray)
          //                }
          //              }
          //            }
          //            complete {
          //              "OK BATCH"
          //            }
          //          }
        }
      } ~
      path("geocode" / Segment) { address =>
        val fParsed: Future[(ParsedAddress, ParsedInputAddress)] = async {
          val addr = await(AddressParserClient.standardize(address))
          if (addr.isLeft) {
            log.error(addr.left.get.desc)
            (ParsedAddress.empty, ParsedInputAddress.empty)
          } else {
            val parsedAddress = addr.right.getOrElse(ParsedAddress.empty)
            val parsedInputAddress = ParsedInputAddress(
              parsedAddress.parts.addressNumber.toInt,
              parsedAddress.parts.streetName.replaceAll(" ", "+"),
              parsedAddress.parts.zip.toInt,
              parsedAddress.parts.state
            )
            (parsedAddress, parsedInputAddress)
          }
        }

        val fGeocoded = async {
          val parsed = await(fParsed)
          val parsedAddress = parsed._1
          val parsedInputAddress = parsed._2

          val ptGeocode = await(AddressPointsClient.geocode(address))
          val addressPointGeocode: AddressPointsResult =
            if (ptGeocode.isLeft) {
              log.error(ptGeocode.left.get.desc)
              AddressPointsResult.error
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
                CensusResult.error
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
