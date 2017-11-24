package utils

import models._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object MyJsonProtocol extends DefaultJsonProtocol {

  implicit val publicationFormat: RootJsonFormat[Publication] = jsonFormat4(Publication.apply)

}
