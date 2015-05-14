package grasshopper.client.parser.protocol

import grasshopper.client.parser.model.{ ParserStatus, AddressPart, ParsedAddress }
import spray.json.DefaultJsonProtocol

trait ParserJsonProtocol extends DefaultJsonProtocol {
  implicit val statusFormat = jsonFormat4(ParserStatus.apply)
  implicit val addressPartFormat = jsonFormat6(AddressPart.apply)
  implicit val parsedAddressFormat = jsonFormat2(ParsedAddress.apply)
}
