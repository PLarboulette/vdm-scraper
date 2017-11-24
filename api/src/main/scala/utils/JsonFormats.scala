package utils

import models.Publication
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


object JsonFormats extends DefaultJsonProtocol  {

  implicit val publicationFormat: RootJsonFormat[Publication] = jsonFormat4(Publication.apply)

}


