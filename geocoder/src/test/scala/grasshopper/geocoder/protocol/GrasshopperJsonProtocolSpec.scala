package grasshopper.geocoder.protocol

import grasshopper.geocoder.model.GeocodeResult
import org.scalatest.{ MustMatchers, FlatSpec }
import spray.json._

class GrasshopperJsonProtocolSpec extends FlatSpec with MustMatchers with GrasshopperJsonProtocol {

  "A Status" must "deserialize from JSON" in {
    val statusStr = """
    {
      "host": "yourhost.local",
      "status": "OK",
      "time": "2015-05-06T19:14:19.304850+00:00",
      "service": "grasshopper-geocoder"
    }"""

    val geocoderStatus = statusStr.parseJson.convertTo[Status]
    geocoderStatus.host mustBe "yourhost.local"
    geocoderStatus.status mustBe "OK"
    geocoderStatus.time mustBe "2015-05-06T19:14:19.304850+00:00"
    geocoderStatus.service mustBe "grasshopper-geocoder"
  }

  it must "serialize to JSON" in {
    val geocoderStatus = Status(
      "OK",
      "grasshopper-census",
      "2015-05-06T19:14:19.304850+00:00",
      "localhost"
    )

    val json = geocoderStatus.toJson.toString
    json.parseJson.convertTo[Status] mustBe geocoderStatus

  }

  "A geocode result" must "serialize from JSON" in {
    val geocodeResultStr = """
       {
         "status": "OK",
         "query": {
           "input": "1311 30th St Washington DC 20007",
           "parts": {
             "AddressNumber": "1311",
             "PlaceName": "washington",
             "StateName": "dc",
             "StreetName": "30th",
             "StreetNamePostType": "st",
             "ZipCode": "20007"
           }
         },
         "features": [
         {
           "service": "census",
           "data": {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [
                  -77.05908853531027,
                  38.90721451814751,
                  0
                ]
              },
              "properties": {
                "RFROMHN": "1301",
                "RTOHN": "1323",
                "ZIPL": "20007",
                "FULLNAME": "30th St NW",
                "LFROMHN": "1300",
                "LTOHN": "1318",
                "ZIPR": "20007",
                "STATE": "DC"
              }
           }
         },
         {
           "service": "addresspoints",
           "data": {
             "type": "Feature",
             "geometry": {
               "type": "Point",
               "coordinates": [
                 -77.05908853531027,
                 38.90721451814751,
                 0
               ]
             },
             "properties": {
               "address": "1311 30th st nw washington dc 20007",
               "alt_address": "",
               "load_date": 1426878185988
             }
           }
         }
         ]
       }
    """

    val geocodeResult = geocodeResultStr.parseJson.convertTo[GeocodeResult]
    geocodeResult.status mustBe "OK"
    geocodeResult.query.parts.AddressNumber mustBe "1311"
    geocodeResult.query.parts.PlaceName mustBe "washington"
    geocodeResult.query.parts.StateName mustBe "dc"
    geocodeResult.query.parts.StreetName mustBe "30th"
    geocodeResult.query.parts.StreetNamePostType mustBe "st"
    geocodeResult.query.parts.ZipCode mustBe "20007"
    geocodeResult.features.size mustBe 2
    geocodeResult.features(0).service mustBe "census"
    geocodeResult.features(1).service mustBe "addresspoints"
    geocodeResult.features(0).data.geometry mustBe geocodeResult.features(1).data.geometry
  }

}
